package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MarketDataService {

    private final ExternalApiCacheRepository externalApiCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    @Value("${alphavantage.api.key}")
    private String alphaVantageApiKey;

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

        try {
            if ("stock".equalsIgnoreCase(type) || "etf".equalsIgnoreCase(type)) {
                // Use Alpha Vantage for stocks/ETFs
                if (alphaVantageApiKey == null || alphaVantageApiKey.isEmpty() || "YOUR_ACTUAL_API_KEY".equals(alphaVantageApiKey)) {
                    logger.error("Alpha Vantage API key is not configured properly in application.properties.");
                    return null;
                }
                url = String.format("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                        ticker, alphaVantageApiKey);
                logger.debug("Fetching from Alpha Vantage: {}", url);
                String response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);

                if (root.has("Error Message")) {
                    logger.error("Alpha Vantage API error for ticker {}: {}", ticker, root.path("Error Message").asText());
                    return null;
                }
                if (root.has("Information")) { // Often indicates rate limit exceeded
                    logger.warn("Alpha Vantage API information for ticker {}: {}", ticker, root.path("Information").asText());
                    return null; // Treat rate limit as a temporary failure
                }

                JsonNode globalQuoteNode = root.path("Global Quote");
                if (globalQuoteNode.isMissingNode() || !globalQuoteNode.isObject() || globalQuoteNode.isEmpty()) {
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
                    BigDecimal value = new BigDecimal(priceStr);
                    return new PriceInfo(value, targetCurrency);
                } catch (NumberFormatException e) {
                    logger.error("Error parsing price '{}' from Alpha Vantage for ticker {}: {}", priceStr, ticker, e.getMessage());
                    return null;
                }

            } else if ("crypto".equalsIgnoreCase(type)) {
                String coinGeckoCurrency = targetCurrency.toLowerCase(); // CoinGecko uses lowercase currency codes
                url = "https://api.coingecko.com/api/v3/simple/price?ids=" + ticker + "&vs_currencies=" + coinGeckoCurrency;
                logger.debug("Fetching from CoinGecko: {}", url);
                String response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);
                JsonNode priceNode = root.path(ticker.toLowerCase()).path(coinGeckoCurrency);
                if (priceNode.isMissingNode()) {
                    logger.warn("Could not find price for crypto ticker: {} in currency {} from CoinGecko response", ticker, coinGeckoCurrency);
                    return null;
                }
                try {
                    BigDecimal value = BigDecimal.valueOf(priceNode.asDouble());
                    return new PriceInfo(value, targetCurrency.toUpperCase());
                } catch (NumberFormatException e) {
                    logger.error("Error parsing price from CoinGecko for ticker {} ({}): {}", ticker, coinGeckoCurrency, e.getMessage());
                    return null;
                }

            } else {
                logger.warn("Unsupported asset type: {}", type);
                return null;
            }
        } catch (IOException e) {
            logger.error("Network or parsing error fetching data for {} (type: {}) from {}: {}", ticker, type, url, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching data for {} (type: {}) from {}: {}", ticker, type, url, e.getMessage());
            return null;
        }
    }
}