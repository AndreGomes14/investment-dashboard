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

        Optional<ExternalApiCache> cacheEntry = externalApiCacheRepository.findByTickerAndType(ticker, type);
        if (cacheEntry.isPresent()) {
            ExternalApiCache cache = cacheEntry.get();
            if (cache.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
                logger.debug("Cache hit for {}:{}. Assuming currency {} from cache.", type, ticker, effectiveTargetCurrency);
                return new PriceInfo(cache.getCurrentValue(), effectiveTargetCurrency);
            }
            logger.debug("Cache expired for {}:{}", type, ticker);
            externalApiCacheRepository.delete(cache);
        }
        logger.debug("Cache miss for {}:{}. Fetching from API.", type, ticker);

        PriceInfo currentPriceInfo = fetchCurrentValueFromAPI(ticker, type, effectiveTargetCurrency);
        if (currentPriceInfo != null) {
            ExternalApiCache newCacheEntry = new ExternalApiCache();
            newCacheEntry.setTicker(ticker);
            newCacheEntry.setType(type);
            newCacheEntry.setCurrentValue(currentPriceInfo.value());
            newCacheEntry.setCurrency(effectiveTargetCurrency);
            newCacheEntry.setLastUpdated(LocalDateTime.now());
            externalApiCacheRepository.save(newCacheEntry);
            logger.debug("Cache updated for {}:{}", type, ticker);
        }
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
        if (alphaVantageApiKey == null || alphaVantageApiKey.isEmpty() || alphaVantageApiKey.equals("YOUR_API_KEY")) {
            logger.error("Alpha Vantage API key is not configured. Please set alphavantage.api.key in application properties.");
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
                    results.add(new InstrumentSearchResult(
                            match.path("1. symbol").asText(null),
                            match.path("2. name").asText(null),
                            match.path("3. type").asText(null),
                            match.path("4. region").asText(null),
                            match.path("8. currency").asText(null)
                    ));
                }
                logger.info("Found {} matches for query '{}'", results.size(), query);
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
}