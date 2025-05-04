package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.investment_dashboard_backend.dto.investment.InstrumentSearchResult;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MarketDataService {

    private final ExternalApiCacheRepository externalApiCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    @Value("${alphavantage.api.key}")
    private String alphaVantageApiKey;

    private final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";

    @Autowired
    public MarketDataService(ExternalApiCacheRepository externalApiCacheRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.externalApiCacheRepository = externalApiCacheRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves the current value and currency of a financial instrument
     * from an external API (Alpha Vantage for stocks/ETFs, CoinGecko for crypto)
     * or the local cache.
     *
     * @param ticker The ticker symbol of the financial instrument.
     * @param type   The type of asset (e.g., "stock", "crypto").
     * @param targetCurrency The expected currency of the investment (used for CoinGecko).
     * @return A PriceInfo object containing the value and currency, or null if the value cannot be retrieved.
     * @throws IOException if there is an error parsing the JSON response.
     */
    public PriceInfo getCurrentValue(String ticker, String type, String targetCurrency) throws IOException {
        if (targetCurrency == null || targetCurrency.trim().isEmpty()) {
            logger.warn("Target currency not provided for {}:{} lookup.", type, ticker);
            return null;
        }
        String effectiveTargetCurrency = targetCurrency.trim().toUpperCase();
        String effectiveType = (type != null) ? type.toLowerCase() : ""; // Normalize type for consistency

        // Find existing cache entry (if any)
        Optional<ExternalApiCache> existingCacheEntryOpt = externalApiCacheRepository.findByTickerAndType(ticker, effectiveType);

        // Check if valid cache entry exists
        if (existingCacheEntryOpt.isPresent()) {
            ExternalApiCache existingCache = existingCacheEntryOpt.get();
            // Check freshness
            if (existingCache.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
                // Check if cached currency matches target currency
                if (effectiveTargetCurrency.equalsIgnoreCase(existingCache.getCurrency())) {
                    logger.debug("Cache hit for {}:{}. Returning cached value.", effectiveType, ticker);
                    return new PriceInfo(existingCache.getCurrentValue(), effectiveTargetCurrency);
                } else {
                    logger.warn("Cache hit for {}:{}, but requested currency {} differs from cached currency {}. Forcing refresh.",
                            effectiveType, ticker, effectiveTargetCurrency, existingCache.getCurrency());
                    // Fall through to fetch from API
                }
            } else {
                logger.debug("Cache expired for {}:{}", effectiveType, ticker);
                // Don't delete, just fall through to fetch and update
            }
        } else {
            logger.debug("Cache miss for {}:{}. Fetching from API.", effectiveType, ticker);
        }

        // Fetch from API if cache missed, expired, or currency mismatch
        PriceInfo currentPriceInfo = fetchCurrentValueFromAPI(ticker, effectiveType, effectiveTargetCurrency);

        if (currentPriceInfo != null) {
            // Perform Upsert (Update or Insert)
            ExternalApiCache cacheToSave;
            if (existingCacheEntryOpt.isPresent()) {
                // Update existing entry
                cacheToSave = existingCacheEntryOpt.get();
                cacheToSave.setType(effectiveType); // Explicitly set normalized type on update too
                logger.debug("Updating existing cache entry for {}:{}", effectiveType, ticker);
            } else {
                // Create new entry
                cacheToSave = new ExternalApiCache();
                cacheToSave.setTicker(ticker);
                cacheToSave.setType(effectiveType); // Use normalized type
                logger.debug("Creating new cache entry for {}:{}", effectiveType, ticker);
            }
            // Set/Update fields common to insert and update
            cacheToSave.setCurrentValue(currentPriceInfo.value());
            cacheToSave.setCurrency(effectiveTargetCurrency); // Store the target currency we used for the fetch
            cacheToSave.setLastUpdated(LocalDateTime.now());

            try {
                externalApiCacheRepository.save(cacheToSave); // Performs INSERT or UPDATE
                logger.debug("Cache saved/updated for {}:{}", effectiveType, ticker);
            } catch (Exception e) {
                // Log potential errors during save (e.g., unexpected constraint violations)
                logger.error("Error saving cache entry for {}:{}: {}", effectiveType, ticker, e.getMessage(), e);
                // Depending on requirements, you might want to still return currentPriceInfo or null here
            }
        } else {
            logger.warn("Failed to fetch current value for {}:{} from API. Cache not updated.", effectiveType, ticker);
            // Optional: If fetch fails but an expired entry exists, maybe return the expired value?
            // if (existingCacheEntryOpt.isPresent()) {
            //     ExternalApiCache expiredCache = existingCacheEntryOpt.get();
            //     logger.warn("Returning expired cache value for {}:{}", type, ticker);
            //     return new PriceInfo(expiredCache.getCurrentValue(), expiredCache.getCurrency());
            // }
        }
        // Return the freshly fetched info (or null if fetch failed)
        return currentPriceInfo;
    }

    /**
     * Fetches the current value from the appropriate external API.
     */
    private PriceInfo fetchCurrentValueFromAPI(String ticker, String type, String targetCurrency) throws IOException {
        String url = null;
        JsonNode root = null;
        String effectiveType = (type != null) ? type.toLowerCase() : ""; // Normalize type

        try {
            // Treat "equity" the same as "stock" for API fetching purposes
            if ("stock".equals(effectiveType) || "etf".equals(effectiveType) || "equity".equals(effectiveType)) {
                // Use Alpha Vantage for stocks/ETFs/Equity
                if (alphaVantageApiKey == null || alphaVantageApiKey.isEmpty() || "YOUR_ACTUAL_API_KEY".equals(alphaVantageApiKey)) {
                    logger.error("Alpha Vantage API key is not configured properly in application.properties.");
                    return null;
                }
                url = String.format("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                        ticker, alphaVantageApiKey);
                logger.debug("Fetching from Alpha Vantage: {}", url);
                String response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);

                // Check for Alpha Vantage API errors or information messages (like rate limits)
                if (root.has("Error Message")) {
                    logger.error("Alpha Vantage API error for ticker {}: {}", ticker, root.path("Error Message").asText());
                    return null;
                }
                if (root.has("Information")) { // Often indicates rate limit exceeded
                    logger.warn("Alpha Vantage API information for ticker {}: {}", ticker, root.path("Information").asText());
                    return null; // Treat rate limit as a temporary failure
                }

                // Navigate the Alpha Vantage JSON structure
                JsonNode globalQuoteNode = root.path("Global Quote");
                if (globalQuoteNode.isMissingNode() || !globalQuoteNode.isObject() || globalQuoteNode.isEmpty()) {
                    // Check if the object is empty, which can happen if the ticker is invalid for AlphaVantage
                    logger.warn("Invalid response structure or empty 'Global Quote' node from Alpha Vantage for ticker: {}.", ticker);
                    return null;
                }

                JsonNode priceNode = globalQuoteNode.path("05. price");
                if (priceNode.isMissingNode() || !priceNode.isTextual()) {
                    logger.warn("Invalid response structure from Alpha Vantage for ticker: {}. '05. price' node missing or not text.", ticker);
                    return null;
                }

                String priceStr = priceNode.asText();
                try {
                    // Assume Alpha Vantage returns price in the targetCurrency (e.g., USD for US stocks)
                    // More complex logic needed if AV returns mixed currencies or if we need conversion here.
                    BigDecimal value = new BigDecimal(priceStr);
                    return new PriceInfo(value, targetCurrency);
                } catch (NumberFormatException e) {
                    logger.error("Error parsing price '{}' from Alpha Vantage for ticker {}: {}", priceStr, ticker, e.getMessage());
                    return null;
                }

            } else if ("crypto".equalsIgnoreCase(effectiveType)) { // Use normalized type
                // Use CoinGecko, requesting the target currency
                String coinGeckoCurrency = targetCurrency.toLowerCase(); // CoinGecko uses lowercase currency codes
                url = "https://api.coingecko.com/api/v3/simple/price?ids=" + ticker + "&vs_currencies=" + coinGeckoCurrency;
                logger.debug("Fetching from CoinGecko: {}", url);
                String response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);
                // Path uses the requested currency code
                JsonNode priceNode = root.path(ticker.toLowerCase()).path(coinGeckoCurrency);
                if (priceNode.isMissingNode()) {
                    logger.warn("Could not find price for crypto ticker: {} in currency {} from CoinGecko response", ticker, coinGeckoCurrency);
                    return null;
                }
                try {
                    BigDecimal value = BigDecimal.valueOf(priceNode.asDouble());
                    return new PriceInfo(value, targetCurrency.toUpperCase()); // Return with uppercase currency code
                } catch (NumberFormatException e) {
                    logger.error("Error parsing price from CoinGecko for ticker {} ({}): {}", ticker, coinGeckoCurrency, e.getMessage());
                    return null;
                }

            } else {
                logger.warn("Unsupported asset type: {}", type); // Log original type if not handled
                return null;
            }
        } catch (IOException e) {
            logger.error("Network or parsing error fetching data for {} (type: {}) from {}: {}", ticker, type, url, e.getMessage());
            throw e; // Re-throw IOExceptions
        } catch (Exception e) {
            logger.error("Unexpected error fetching data for {} (type: {}) from {}: {}", ticker, type, url, e.getMessage());
            return null; // Return null for other unexpected errors
        }
    }

    /**
     * Searches for financial instruments using the Alpha Vantage API.
     * @param query The search keyword string.
     * @return A list of matching instruments.
     */
    public List<InstrumentSearchResult> searchInstruments(String query) {
        List<InstrumentSearchResult> alphaVantageResults = searchAlphaVantage(query);
        List<InstrumentSearchResult> coinGeckoResults = searchCoinGecko(query);

        // Combine the results
        List<InstrumentSearchResult> combinedResults = new ArrayList<>(alphaVantageResults);
        combinedResults.addAll(coinGeckoResults);

        // Optional: Add logic here to remove duplicates or prioritize one source if needed
        logger.info("Combined search results for query '{}': Found {} from AlphaVantage, {} from CoinGecko. Total: {}",
                query, alphaVantageResults.size(), coinGeckoResults.size(), combinedResults.size());

        return combinedResults;
    }

    /**
     * Helper method to search Alpha Vantage.
     */
    private List<InstrumentSearchResult> searchAlphaVantage(String query) {
        if (alphaVantageApiKey == null || alphaVantageApiKey.isEmpty() || "YOUR_ACTUAL_API_KEY".equals(alphaVantageApiKey)) {
            logger.error("Alpha Vantage API key is not configured. Cannot search Alpha Vantage.");
            return Collections.emptyList();
        }

        String url = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_BASE_URL)
                .queryParam("function", "SYMBOL_SEARCH")
                .queryParam("keywords", query)
                .queryParam("apikey", alphaVantageApiKey)
                .toUriString();

        logger.info("Searching Alpha Vantage for instruments matching: {}", query);

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode bestMatches = rootNode.path("bestMatches");

            if (bestMatches.isArray()) {
                List<InstrumentSearchResult> results = new ArrayList<>();
                for (JsonNode match : bestMatches) {
                    // Skip if symbol is missing or empty
                    String symbol = match.path("1. symbol").asText(null);
                    if (symbol == null || symbol.trim().isEmpty()) {
                        continue;
                    }
                    results.add(new InstrumentSearchResult(
                            symbol,
                            match.path("2. name").asText(null),
                            match.path("3. type").asText(null), // e.g., Equity, ETF
                            match.path("4. region").asText(null),
                            match.path("8. currency").asText(null)
                    ));
                }
                logger.debug("Alpha Vantage search for '{}' found {} matches.", query, results.size());
                return results;
            } else {
                logger.warn("Alpha Vantage search response for query '{}' did not contain 'bestMatches'. Response: {}", query, jsonResponse);
                if(rootNode.has("Note")) {
                    logger.warn("Alpha Vantage API Note: {}", rootNode.path("Note").asText());
                }
                return Collections.emptyList();
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error searching Alpha Vantage instruments for '{}': {} - {}", query, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Collections.emptyList();
        } catch (IOException e) {
            logger.error("Error parsing Alpha Vantage search response for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Unexpected error searching Alpha Vantage instruments for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Helper method to search CoinGecko for cryptocurrencies.
     */
    private List<InstrumentSearchResult> searchCoinGecko(String query) {
        String url = UriComponentsBuilder.fromHttpUrl("https://api.coingecko.com/api/v3/search")
                .queryParam("query", query)
                .toUriString();

        logger.info("Searching CoinGecko for instruments matching: {}", query);

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode coinsNode = rootNode.path("coins");

            if (coinsNode.isArray()) {
                List<InstrumentSearchResult> results = new ArrayList<>();
                for (JsonNode coin : coinsNode) {
                    // Use CoinGecko ID as the 'ticker' for our system
                    // Use symbol for display perhaps, but ID is needed for price lookups
                    String coinGeckoId = coin.path("id").asText(null);
                    String symbol = coin.path("symbol").asText(null);
                    if (coinGeckoId == null || coinGeckoId.trim().isEmpty()) {
                        continue; // Skip if essential info missing
                    }
                    results.add(new InstrumentSearchResult(
                            coinGeckoId, // Use CoinGecko ID as the ticker
                            coin.path("name").asText(null),
                            "Crypto", // Set type explicitly
                            null, // Region not available from this endpoint
                            null // Currency not available from this endpoint
                    ));
                }
                logger.debug("CoinGecko search for '{}' found {} coin matches.", query, results.size());
                return results;
            } else {
                logger.warn("CoinGecko search response for query '{}' did not contain 'coins' array. Response: {}", query, jsonResponse);
                return Collections.emptyList();
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error searching CoinGecko instruments for '{}': {} - {}", query, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Collections.emptyList();
        } catch (IOException e) {
            logger.error("Error parsing CoinGecko search response for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Unexpected error searching CoinGecko instruments for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}