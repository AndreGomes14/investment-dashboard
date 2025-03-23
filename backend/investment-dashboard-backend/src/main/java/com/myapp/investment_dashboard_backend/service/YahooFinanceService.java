package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.config.YahooFinanceConfig;
import com.myapp.investment_dashboard_backend.dto.yahoo.YahooFinanceQuote;
import com.myapp.investment_dashboard_backend.dto.yahoo.YahooFinanceResponse;
import com.myapp.investment_dashboard_backend.exception.ExternalApiException;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.InvestmentType;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for interacting with the Yahoo Finance API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceService {
    
    private final RestTemplate restTemplate;
    private final ExternalApiCacheRepository apiCacheRepository;
    
    // Cache TTL in minutes
    private static final int CACHE_TTL_MINUTES = 15;
    
    /**
     * Fetch current investment prices from Yahoo Finance
     * @param investment The investment entity
     * @return YahooFinanceQuote containing price data
     * @throws ExternalApiException if API call fails
     */
    public YahooFinanceQuote fetchInvestmentPrice(Investment investment) throws ExternalApiException {
        if (investment == null || investment.getTicker() == null) {
            throw new IllegalArgumentException("Investment or ticker is null");
        }
        
        String ticker = investment.getTicker();
        
        // Check if we can use Yahoo Finance for this investment type
        try {
            InvestmentType type = InvestmentType.valueOf(investment.getType());
            if (!InvestmentType.isYahooFinanceSupported(type)) {
                log.warn("Investment type {} is not supported by Yahoo Finance", type);
                throw new ExternalApiException("Investment type " + type + " is not supported by Yahoo Finance");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown investment type: {}", investment.getType());
        }
        
        // Check cache first
        Optional<ExternalApiCache> cachedData = apiCacheRepository.findByKeyAndProvider(
            ticker, "YAHOO_FINANCE");
        
        if (cachedData.isPresent() && !isCacheExpired(cachedData.get())) {
            try {
                YahooFinanceQuote quote = new YahooFinanceQuote();
                quote.setSymbol(ticker);
                quote.setPrice(new BigDecimal(cachedData.get().getData()));
                quote.setTimestamp(cachedData.get().getLastUpdated());
                return quote;
            } catch (Exception e) {
                log.warn("Error parsing cached data for {}: {}", ticker, e.getMessage());
                // Continue to fetch fresh data
            }
        }
        
        try {
            // Build URL for Yahoo Finance API
            String url = YahooFinanceConfig.YAHOO_FINANCE_API_BASE_URL + 
                         ticker + 
                         YahooFinanceConfig.YAHOO_FINANCE_API_QUERY_PARAMS;
            
            // Make the API call
            ResponseEntity<YahooFinanceResponse> response = 
                restTemplate.getForEntity(url, YahooFinanceResponse.class);
            
            if (response.getBody() == null || response.getBody().getChart() == null) {
                throw new ExternalApiException("Null response from Yahoo Finance API");
            }
            
            YahooFinanceResponse yahooResponse = response.getBody();
            
            // Check for errors in response
            if (yahooResponse.getChart().getError() != null) {
                throw new ExternalApiException("Yahoo Finance API error: " + 
                    yahooResponse.getChart().getError().getDescription());
            }
            
            if (yahooResponse.getChart().getResult() == null || 
                yahooResponse.getChart().getResult().isEmpty()) {
                throw new ExternalApiException("No results found for ticker: " + ticker);
            }
            
            // Extract data from the response
            YahooFinanceResponse.Result result = yahooResponse.getChart().getResult().get(0);
            YahooFinanceResponse.Meta meta = result.getMeta();
            
            // Create and populate the quote
            YahooFinanceQuote quote = new YahooFinanceQuote();
            quote.setSymbol(meta.getSymbol());
            quote.setRegularMarketPrice(BigDecimal.valueOf(meta.getRegularMarketPrice()));
            quote.setPreviousClose(BigDecimal.valueOf(meta.getPreviousClose()));
            quote.setCurrency(meta.getCurrency());
            quote.setRegularMarketTime(meta.getRegularMarketTime());
            
            // Calculate change and percent change
            double change = meta.getRegularMarketPrice() - meta.getPreviousClose();
            double percentChange = (change / meta.getPreviousClose()) * 100;
            
            quote.setRegularMarketChange(BigDecimal.valueOf(change));
            quote.setRegularMarketChangePercent(BigDecimal.valueOf(percentChange));
            
            // Update cache
            updateCache(ticker, meta.getRegularMarketPrice().toString());
            
            return quote;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching data for {}: {}", ticker, e.getMessage());
            throw new ExternalApiException("Error from Yahoo Finance API: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching data for {}: {}", ticker, e.getMessage());
            throw new ExternalApiException("Failed to fetch data from Yahoo Finance: " + e.getMessage());
        }
    }
    
    /**
     * Update the API cache with new data
     * @param ticker The investment ticker symbol
     * @param data The data to cache (price as string)
     */
    private void updateCache(String ticker, String data) {
        try {
            Optional<ExternalApiCache> existingCache = 
                apiCacheRepository.findByKeyAndProvider(ticker, "YAHOO_FINANCE");
            
            if (existingCache.isPresent()) {
                ExternalApiCache cache = existingCache.get();
                cache.setData(data);
                cache.setLastUpdated(LocalDateTime.now());
                apiCacheRepository.save(cache);
            } else {
                ExternalApiCache newCache = new ExternalApiCache();
                newCache.setKey(ticker);
                newCache.setProvider("YAHOO_FINANCE");
                newCache.setData(data);
                newCache.setLastUpdated(LocalDateTime.now());
                apiCacheRepository.save(newCache);
            }
        } catch (Exception e) {
            log.error("Error updating cache for {}: {}", ticker, e.getMessage());
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
     * Batch fetch current prices for multiple tickers
     * @param tickers List of ticker symbols
     * @return Map of ticker to quote data
     */
    public Map<String, YahooFinanceQuote> batchFetchPrices(Iterable<String> tickers) {
        Map<String, YahooFinanceQuote> results = new HashMap<>();
        
        for (String ticker : tickers) {
            try {
                Investment dummyInvestment = new Investment();
                dummyInvestment.setTicker(ticker);
                dummyInvestment.setType(InvestmentType.STOCK.name()); // Assuming stock as default
                
                YahooFinanceQuote quote = fetchInvestmentPrice(dummyInvestment);
                results.put(ticker, quote);
            } catch (Exception e) {
                log.error("Error fetching data for {}: {}", ticker, e.getMessage());
                // Continue with next ticker
            }
        }
        
        return results;
    }
} 