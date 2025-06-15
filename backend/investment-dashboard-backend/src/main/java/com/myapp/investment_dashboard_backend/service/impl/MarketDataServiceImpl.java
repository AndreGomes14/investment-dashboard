package com.myapp.investment_dashboard_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.investment_dashboard_backend.dto.investment.InstrumentSearchResult;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.service.MarketDataService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class MarketDataServiceImpl implements MarketDataService {

    private final ExternalApiCacheRepository externalApiCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataServiceImpl.class);

    @Value("${alphavantage.api.key}")
    private String alphaVantageApiKey;

    private static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";
    private static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3";

    // Strategy map for fetching prices
    private final Map<String, InternalPriceFetcher> priceFetcherStrategies;

    // Functional interface for price fetching strategies
    @FunctionalInterface
    private interface InternalPriceFetcher {
        PriceInfo fetch(String ticker, String targetCurrency) throws IOException;
    }

    @Autowired
    public MarketDataServiceImpl(ExternalApiCacheRepository externalApiCacheRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.externalApiCacheRepository = externalApiCacheRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        // Initialize price fetcher strategies
        this.priceFetcherStrategies = new HashMap<>();
        this.priceFetcherStrategies.put("stock", this::fetchFromAlphaVantage);
        this.priceFetcherStrategies.put("etf", this::fetchFromAlphaVantage);
        this.priceFetcherStrategies.put("equity", this::fetchFromAlphaVantage);
        this.priceFetcherStrategies.put("crypto", this::fetchFromCoinGecko);
    }

    @Override
    public PriceInfo getCurrentValue(String ticker, String type, String targetCurrency) throws IOException {
        return getCurrentValue(ticker, type, targetCurrency, false);
    }

    @Override
    public PriceInfo getCurrentValue(String ticker, String type, String targetCurrency, boolean forceRefresh) throws IOException {
        if (targetCurrency == null || targetCurrency.trim().isEmpty()) {
            logger.warn("Target currency not provided for {}:{} lookup.", type, ticker);
            return null;
        }
        String effectiveTargetCurrency = targetCurrency.trim().toUpperCase();
        String effectiveType = (type != null) ? type.toLowerCase() : "";

        Optional<ExternalApiCache> existingCacheEntryOpt = externalApiCacheRepository.findByTickerAndType(ticker, effectiveType);

        if (!forceRefresh && existingCacheEntryOpt.isPresent()) {
            ExternalApiCache existingCache = existingCacheEntryOpt.get();
            if (isCacheValid(existingCache, effectiveTargetCurrency)) {
                logger.debug("Cache hit for {}:{}. Returning cached value.", effectiveType, ticker);
                return new PriceInfo(existingCache.getCurrentValue(), effectiveTargetCurrency);
            }
        }

        logger.debug("Cache miss, invalid, or force refresh for {}:{}. Fetching from API.", effectiveType, ticker);
        PriceInfo currentPriceInfo = fetchCurrentValueFromAPI(ticker, effectiveType, effectiveTargetCurrency);

        if (currentPriceInfo != null) {
            upsertCacheEntry(ticker, effectiveType, currentPriceInfo, effectiveTargetCurrency, existingCacheEntryOpt);
        }
        return currentPriceInfo;
    }

    private boolean isCacheValid(ExternalApiCache cacheEntry, String targetCurrency) {
        if (cacheEntry.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
            if (targetCurrency.equalsIgnoreCase(cacheEntry.getCurrency())) {
                return true;
            } else {
                logger.warn("Cache hit for {}:{}, but requested currency {} differs from cached currency {}. Forcing refresh.",
                        cacheEntry.getType(), cacheEntry.getTicker(), targetCurrency, cacheEntry.getCurrency());
                return false;
            }
        }
        logger.debug("Cache expired for {}:{}", cacheEntry.getType(), cacheEntry.getTicker());
        return false;
    }

    private void upsertCacheEntry(String ticker, String type, PriceInfo priceInfo, String currency, Optional<ExternalApiCache> existingEntryOpt) {
        ExternalApiCache cacheToSave = existingEntryOpt.orElseGet(() -> {
            logger.debug("Creating new cache entry for {}:{}", type, ticker);
            ExternalApiCache newCache = new ExternalApiCache();
            newCache.setTicker(ticker);
            newCache.setType(type);
            return newCache;
        });

        if (existingEntryOpt.isPresent()) {
            logger.debug("Updating existing cache entry for {}:{}", type, ticker);
            cacheToSave.setType(type); 
        }

        cacheToSave.setCurrentValue(priceInfo.value());
        cacheToSave.setCurrency(currency);
        cacheToSave.setLastUpdated(LocalDateTime.now());

        try {
            externalApiCacheRepository.save(cacheToSave);
            logger.debug("Cache saved/updated for {}:{}", type, ticker);
        } catch (Exception e) {
            logger.error("Error saving cache entry for {}:{}: {}", type, ticker, e.getMessage(), e);
        }
    }

    // Generic helper to execute GET request and parse JSON response
    private <T> T executeGetAndParse(
            String url,
            String logContext,
            Function<JsonNode, T> jsonProcessor,
            Supplier<T> defaultValueSupplier
    ) throws IOException {
        logger.debug("Fetching data for {}: {}", logContext, url);
        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            if (jsonResponse == null) {
                logger.warn("No response received from API for {}: {}", logContext, url);
                return defaultValueSupplier.get();
            }
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            return jsonProcessor.apply(rootNode);
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error for {}: {} - {}. URL: {}", logContext, e.getStatusCode(), e.getResponseBodyAsString(), url, e);
            return defaultValueSupplier.get();
        } catch (IOException e) {
            logger.error("IOException (network/parsing) for {}: {}. URL: {}", logContext, e.getMessage(), url, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error for {}: {}. URL: {}", logContext, e.getMessage(), url, e);
            return defaultValueSupplier.get();
        }
    }

    private PriceInfo fetchCurrentValueFromAPI(String ticker, String originalType, String targetCurrency) throws IOException {
        // 'originalType' is passed for logging, 'effectiveType' for map lookup
        String effectiveType = (originalType != null) ? originalType.toLowerCase() : "";
        InternalPriceFetcher fetcher = priceFetcherStrategies.get(effectiveType);

        if (fetcher != null) {
            return fetcher.fetch(ticker, targetCurrency);
        } else {
            logger.warn("Unsupported asset type: {}", originalType); // Log the original type
            return null;
        }
    }

    private PriceInfo fetchFromAlphaVantage(String ticker, String targetCurrency) throws IOException {
        if (isAlphaVantageKeyInvalid()) {
            logger.error("Alpha Vantage API key is not configured properly.");
            return null;
        }
        String url = buildAlphaVantageQuoteUrl(ticker);
        String logContext = String.format("AlphaVantage Global Quote for %s", ticker);

        return executeGetAndParse(url, logContext,
            rootNode -> parseAlphaVantagePriceResponse(rootNode, ticker, targetCurrency),
            () -> null);
    }

    private static PriceInfo parseAlphaVantagePriceResponse(JsonNode rootNode, String ticker, String targetCurrency) {
        if (rootNode.has("Error Message")) {
            logger.error("Alpha Vantage API error for ticker {}: {}", ticker, rootNode.path("Error Message").asText());
            return null;
        }
        if (rootNode.has("Information")) {
            logger.warn("Alpha Vantage API information for ticker {}: {}", ticker, rootNode.path("Information").asText());
            return null;
        }

        JsonNode globalQuoteNode = rootNode.path("Global Quote");
        if (globalQuoteNode.isMissingNode() || !globalQuoteNode.isObject() || globalQuoteNode.isEmpty()) {
            logger.warn("Invalid response structure or empty 'Global Quote' node from Alpha Vantage for ticker: {}.", ticker);
            return null;
        }

        JsonNode priceNode = globalQuoteNode.path("05. price");
        if (priceNode.isMissingNode() || !priceNode.isTextual()) {
            logger.warn("Invalid response structure from Alpha Vantage for ticker: {}. '05. price' node missing or not text.", ticker);
            return null;
        }

        try {
            String priceStr = priceNode.asText();
            BigDecimal value = new BigDecimal(priceStr);
            return new PriceInfo(value, targetCurrency);
        } catch (NumberFormatException e) {
            logger.error("Error parsing price from Alpha Vantage for ticker {}: {}", ticker, e.getMessage());
            return null;
        }
    }

    private PriceInfo fetchFromCoinGecko(String ticker, String targetCurrency) throws IOException {
        String coinGeckoCurrency = targetCurrency.toLowerCase();
        String url = buildCoinGeckoPriceUrl(ticker, coinGeckoCurrency);
        String logContext = String.format("CoinGecko Price for %s in %s", ticker, coinGeckoCurrency);

        return executeGetAndParse(url, logContext,
            rootNode -> parseCoinGeckoPriceResponse(rootNode, ticker, targetCurrency, coinGeckoCurrency),
            () -> null);
    }

    private static PriceInfo parseCoinGeckoPriceResponse(JsonNode rootNode, String ticker, String targetCurrency, String coinGeckoCurrency) {
        JsonNode priceNode = rootNode.path(ticker.toLowerCase()).path(coinGeckoCurrency);
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
    }

    @Override
    public List<InstrumentSearchResult> searchInstruments(String query) {
        List<InstrumentSearchResult> alphaVantageResults = searchAlphaVantage(query);
        List<InstrumentSearchResult> coinGeckoResults = searchCoinGecko(query);

        List<InstrumentSearchResult> combinedResults = new ArrayList<>(alphaVantageResults);
        combinedResults.addAll(coinGeckoResults);

        logger.info("Combined search results for query '{}': Found {} from AlphaVantage, {} from CoinGecko. Total: {}",
                query, alphaVantageResults.size(), coinGeckoResults.size(), combinedResults.size());

        return combinedResults;
    }

    private List<InstrumentSearchResult> searchAlphaVantage(String query) {
        if (isAlphaVantageKeyInvalid()) {
            logger.error("Alpha Vantage API key is not configured. Cannot search Alpha Vantage.");
            return Collections.emptyList();
        }
        String url = buildAlphaVantageSearchUrl(query);
        String logContext = String.format("AlphaVantage Search for '%s'", query);

        try {
            return executeGetAndParse(url, logContext,
                rootNode -> parseAlphaVantageSearchResponse(rootNode, query),
                Collections::emptyList);
        } catch (IOException e) {
            logger.error("IOException during Alpha Vantage search for query '{}', returning empty list.", query, e);
            return Collections.emptyList();
        }
    }

    private static List<InstrumentSearchResult> parseAlphaVantageSearchResponse(JsonNode rootNode, String query) {
        JsonNode bestMatches = rootNode.path("bestMatches");
        if (bestMatches.isArray()) {
            List<InstrumentSearchResult> results = new ArrayList<>();
            for (JsonNode match : bestMatches) {
                String symbol = match.path("1. symbol").asText(null);
                if (symbol == null || symbol.trim().isEmpty()) {
                    continue;
                }
                results.add(new InstrumentSearchResult(
                        symbol,
                        match.path("2. name").asText(null),
                        match.path("3. type").asText(null),
                        match.path("4. region").asText(null),
                        match.path("8. currency").asText(null)
                ));
            }
            logger.debug("Alpha Vantage search for '{}' found {} matches.", query, results.size());
            return results;
        } else {
            logger.warn("Alpha Vantage search response for query '{}' did not contain 'bestMatches'. Response: {}", query, rootNode.toString().substring(0, Math.min(rootNode.toString().length(), 200)));
            if (rootNode.has("Note")) {
                logger.warn("Alpha Vantage API Note: {}", rootNode.path("Note").asText());
            }
            return Collections.emptyList();
        }
    }

    private List<InstrumentSearchResult> searchCoinGecko(String query) {
        String url = buildCoinGeckoSearchUrl(query);
        String logContext = String.format("CoinGecko Search for '%s'", query);

        try {
            return executeGetAndParse(url, logContext,
                rootNode -> parseCoinGeckoSearchResponse(rootNode, query),
                Collections::emptyList);
        } catch (IOException e) {
            logger.error("IOException during CoinGecko search for query '{}', returning empty list.", query, e);
            return Collections.emptyList();
        }
    }

    private static List<InstrumentSearchResult> parseCoinGeckoSearchResponse(JsonNode rootNode, String query) {
        JsonNode coinsNode = rootNode.path("coins");
        if (coinsNode.isArray()) {
            List<InstrumentSearchResult> results = new ArrayList<>();
            for (JsonNode coin : coinsNode) {
                String coinGeckoId = coin.path("id").asText(null);
                if (coinGeckoId == null || coinGeckoId.trim().isEmpty()) {
                    continue;
                }
                results.add(new InstrumentSearchResult(
                        coinGeckoId,
                        coin.path("name").asText(null),
                        "Crypto",
                        null,
                        null
                ));
            }
            logger.debug("CoinGecko search for '{}' found {} coin matches.", query, results.size());
            return results;
        } else {
            logger.warn("CoinGecko search response for query '{}' did not contain 'coins' array. Response: {}", query, rootNode.toString().substring(0, Math.min(rootNode.toString().length(), 200)));
            return Collections.emptyList();
        }
    }

    // Helper methods for API key validation and URL building

    private boolean isAlphaVantageKeyInvalid() {
        return alphaVantageApiKey == null || alphaVantageApiKey.isEmpty() || "YOUR_ACTUAL_API_KEY".equals(alphaVantageApiKey);
    }

    private String buildAlphaVantageQuoteUrl(String ticker) {
        return UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_BASE_URL)
                .queryParam("function", "GLOBAL_QUOTE")
                .queryParam("symbol", ticker)
                .queryParam("apikey", alphaVantageApiKey)
                .toUriString();
    }

    private String buildAlphaVantageSearchUrl(String keywords) {
        return UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_BASE_URL)
                .queryParam("function", "SYMBOL_SEARCH")
                .queryParam("keywords", keywords)
                .queryParam("apikey", alphaVantageApiKey)
                .toUriString();
    }

    private String buildCoinGeckoPriceUrl(String coinId, String vsCurrency) {
        return UriComponentsBuilder.fromHttpUrl(COINGECKO_BASE_URL)
                .path("/simple/price")
                .queryParam("ids", coinId)
                .queryParam("vs_currencies", vsCurrency)
                .toUriString();
    }

    private String buildCoinGeckoSearchUrl(String query) {
        return UriComponentsBuilder.fromHttpUrl(COINGECKO_BASE_URL)
                .path("/search")
                .queryParam("query", query)
                .toUriString();
    }
}