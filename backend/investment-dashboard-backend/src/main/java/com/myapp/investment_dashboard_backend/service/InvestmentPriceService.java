package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.yahoo.YahooFinanceQuote;
import com.myapp.investment_dashboard_backend.exception.ExternalApiException;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.InvestmentType;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service to manage investment price fetching from multiple sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentPriceService {
    
    private final YahooFinanceService yahooFinanceService;
    private final CryptoPriceService cryptoPriceService;
    private final InvestmentRepository investmentRepository;
    
    // Thread pool for parallel price updates
    private final Executor executor = Executors.newFixedThreadPool(10);
    
    /**
     * Update the current value of an investment
     * @param investment The investment to update
     * @return true if update was successful, false otherwise
     */
    public boolean updateInvestmentCurrentValue(Investment investment) {
        try {
            if (investment == null || investment.getTicker() == null) {
                log.warn("Cannot update value for null investment or ticker");
                return false;
            }
            
            YahooFinanceQuote quote;
            
            // Determine which API to use based on investment type
            try {
                InvestmentType type = InvestmentType.valueOf(investment.getType());
                
                if (type == InvestmentType.CRYPTO) {
                    quote = cryptoPriceService.fetchCryptoPrice(investment);
                } else {
                    quote = yahooFinanceService.fetchInvestmentPrice(investment);
                }
                
                // Update the investment with new price data
                BigDecimal currentPrice = quote.getCurrentPrice();
                BigDecimal amount = investment.getAmount();
                
                investment.setCurrentValue(currentPrice.multiply(amount));
                investment.setLastUpdateDate(LocalDateTime.now());
                
                // Save the updated investment
                investmentRepository.save(investment);
                
                log.info("Updated value for investment {}: {} {} ({})", 
                    investment.getTicker(), 
                    investment.getCurrentValue(), 
                    quote.getCurrency(),
                    investment.getLastUpdateDate());
                
                return true;
            } catch (IllegalArgumentException e) {
                log.warn("Unknown investment type: {}", investment.getType());
                // Attempt to use Yahoo Finance as fallback
                quote = yahooFinanceService.fetchInvestmentPrice(investment);
                
                // Update if we got a quote from the fallback
                BigDecimal currentPrice = quote.getCurrentPrice();
                BigDecimal amount = investment.getAmount();
                
                investment.setCurrentValue(currentPrice.multiply(amount));
                investment.setLastUpdateDate(LocalDateTime.now());
                
                investmentRepository.save(investment);
                return true;
            }
        } catch (ExternalApiException e) {
            log.error("Error updating value for investment {}: {}", 
                investment.getTicker(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error updating investment value: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Update all investments in a portfolio
     * @param portfolio The portfolio containing investments to update
     * @return Number of successfully updated investments
     */
    public int updatePortfolioInvestments(Portfolio portfolio) {
        List<Investment> investments = investmentRepository
            .findByPortfolioAndStatus(portfolio, "ACTIVE");
        
        if (investments.isEmpty()) {
            log.info("No active investments found for portfolio {}", portfolio.getId());
            return 0;
        }
        
        // Run updates in parallel
        List<CompletableFuture<Boolean>> futures = investments.stream()
            .map(investment -> CompletableFuture.supplyAsync(
                () -> updateInvestmentCurrentValue(investment), executor))
            .collect(Collectors.toList());
        
        // Wait for all updates to complete and count successes
        return (int) futures.stream()
            .map(CompletableFuture::join)
            .filter(Boolean::booleanValue)
            .count();
    }
    
    /**
     * Scheduled task to update all active investments
     * Runs every 15 minutes by default
     */
    @Scheduled(fixedRateString = "${investment.price.update.interval:900000}")
    public void scheduledInvestmentUpdate() {
        log.info("Starting scheduled investment value update");
        
        List<Investment> activeInvestments = investmentRepository.findByStatus("ACTIVE");
        
        log.info("Found {} active investments to update", activeInvestments.size());
        
        // Create batches to process - we want some parallelism but not too many concurrent requests
        List<List<Investment>> batches = createBatches(activeInvestments, 10);
        
        int totalUpdated = 0;
        
        for (List<Investment> batch : batches) {
            // Process each batch in parallel
            List<CompletableFuture<Boolean>> futures = batch.stream()
                .map(investment -> CompletableFuture.supplyAsync(
                    () -> updateInvestmentCurrentValue(investment), executor))
                .collect(Collectors.toList());
            
            // Wait for the batch to complete
            int batchUpdated = (int) futures.stream()
                .map(CompletableFuture::join)
                .filter(Boolean::booleanValue)
                .count();
            
            totalUpdated += batchUpdated;
        }
        
        log.info("Scheduled update completed. Updated {} out of {} investments", 
            totalUpdated, activeInvestments.size());
    }
    
    /**
     * Helper method to create batches from a list
     * @param list The full list
     * @param batchSize The maximum batch size
     * @param <T> The list element type
     * @return A list of batches
     */
    private <T> List<List<T>> createBatches(List<T> list, int batchSize) {
        return list.stream()
            .collect(Collectors.groupingBy(
                item -> list.indexOf(item) / batchSize))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
} 