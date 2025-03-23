package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.myapp.investment_dashboard_backend.dto.yahoo.YahooFinanceQuote;
import com.myapp.investment_dashboard_backend.exception.ExternalApiException;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for fetching cryptocurrency prices from CoinGecko API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoPriceService {
    
    private final RestTemplate restTemplate;
    private final ExternalApiCacheRepository apiCacheRepository;
    
    // CoinGecko API base URL
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";
    private static final String PRICE_ENDPOINT = "/simple/price";
    
    // Cache TTL in minutes
    private static final int CACHE_TTL_MINUTES = 5; // Shorter TTL for crypto due to volatility
    
    /**
     * Fetch current cryptocurrency price
     * @param investment The investment entity (must have type CRYPTO)
     * @return YahooFinanceQuote with price data (for consistency with stock quotes)
     * @throws ExternalApiException if API call fails
     */
    public YahooFinanceQuote fetchCryptoPrice(Investment investment) throws ExternalApiException {
        if (investment == null || investment.getTicker() == null) {
            throw new IllegalArgumentException("Investment or ticker is null");
        }
        
        String ticker = investment.getTicker().toLowerCase();
        
        // Check cache first
        Optional<ExternalApiCache> cachedData = apiCacheRepository.findByKeyAndProvider(
            ticker, "COINGECKO");
        
        if (cachedData.isPresent() && !isCacheExpired(cachedData.get())) {
            try {
                YahooFinanceQuote quote = new YahooFinanceQuote();
                quote.setSymbol(ticker.toUpperCase());
                quote.setPrice(new BigDecimal(cachedData.get().getData()));
                quote.setTimestamp(cachedData.get().getLastUpdated());
                return quote;
            } catch (Exception e) {
                log.warn("Error parsing cached data for crypto {}: {}", ticker, e.getMessage());
                // Continue to fetch fresh data
            }
        }
        
        try {
            // Convert ticker to CoinGecko ID if needed (e.g., BTC -> bitcoin)
            String coinId = mapTickerToCoinGeckoId(ticker);
            
            // Build URL for CoinGecko API
            String url = COINGECKO_API_URL + PRICE_ENDPOINT + 
                        "?ids=" + coinId + "&vs_currencies=usd";
            
            // Make the API call
            ResponseEntity<JsonNode> response = 
                restTemplate.getForEntity(url, JsonNode.class);
            
            if (response.getBody() == null || response.getBody().isEmpty()) {
                throw new ExternalApiException("Null response from CoinGecko API");
            }
            
            JsonNode responseBody = response.getBody();
            
            // Extract price data
            if (!responseBody.has(coinId) || !responseBody.get(coinId).has("usd")) {
                throw new ExternalApiException("Price data not found for " + coinId);
            }
            
            BigDecimal price = BigDecimal.valueOf(responseBody.get(coinId).get("usd").asDouble());
            
            // Create and populate the quote (using YahooFinanceQuote for consistency)
            YahooFinanceQuote quote = new YahooFinanceQuote();
            quote.setSymbol(ticker.toUpperCase());
            quote.setPrice(price);
            quote.setTimestamp(LocalDateTime.now());
            quote.setCurrency("USD");
            
            // Update cache
            updateCache(ticker, price.toString());
            
            return quote;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching crypto data for {}: {}", ticker, e.getMessage());
            throw new ExternalApiException("Error from CoinGecko API: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching crypto data for {}: {}", ticker, e.getMessage());
            throw new ExternalApiException("Failed to fetch data from CoinGecko: " + e.getMessage());
        }
    }
    
    /**
     * Update the API cache with new data
     * @param ticker The crypto ticker symbol
     * @param data The data to cache (price as string)
     */
    private void updateCache(String ticker, String data) {
        try {
            Optional<ExternalApiCache> existingCache = 
                apiCacheRepository.findByKeyAndProvider(ticker, "COINGECKO");
            
            if (existingCache.isPresent()) {
                ExternalApiCache cache = existingCache.get();
                cache.setData(data);
                cache.setLastUpdated(LocalDateTime.now());
                apiCacheRepository.save(cache);
            } else {
                ExternalApiCache newCache = new ExternalApiCache();
                newCache.setKey(ticker);
                newCache.setProvider("COINGECKO");
                newCache.setData(data);
                newCache.setLastUpdated(LocalDateTime.now());
                apiCacheRepository.save(newCache);
            }
        } catch (Exception e) {
            log.error("Error updating cache for crypto {}: {}", ticker, e.getMessage());
            // Continue execution even if cache update fails
        }
    }
    
    /**
     * Check if the cached data is expired
     * @param cache The cache entity to check
     * @return true if expired, false otherwise
     */
    private boolean isCacheExpired(ExternalApiCache cache) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = cache.getLastUpdated().plusMinutes(CACHE_TTL_MINUTES);
        return now.isAfter(expiry);
    }
    
    /**
     * Map common crypto tickers to CoinGecko IDs
     * @param ticker The crypto ticker (e.g., BTC, ETH)
     * @return The corresponding CoinGecko ID (e.g., bitcoin, ethereum)
     */
    private String mapTickerToCoinGeckoId(String ticker) {
        ticker = ticker.toUpperCase();
        
        switch (ticker) {
            case "BTC":
                return "bitcoin";
            case "ETH":
                return "ethereum";
            case "XRP":
                return "ripple";
            case "LTC":
                return "litecoin";
            case "BCH":
                return "bitcoin-cash";
            case "ADA":
                return "cardano";
            case "DOT":
                return "polkadot";
            case "LINK":
                return "chainlink";
            case "XLM":
                return "stellar";
            case "DOGE":
                return "dogecoin";
            case "UNI":
                return "uniswap";
            case "AAVE":
                return "aave";
            case "SOL":
                return "solana";
            case "AVAX":
                return "avalanche-2";
            default:
                return ticker.toLowerCase(); // Default to lowercase
        }
    }
} 