package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.dto.ApiResponse;
import com.myapp.investment_dashboard_backend.dto.yahoo.YahooFinanceQuote;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.service.CryptoPriceService;
import com.myapp.investment_dashboard_backend.service.InvestmentPriceService;
import com.myapp.investment_dashboard_backend.service.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for investment price-related operations
 */
@RestController
@RequestMapping("/api/investment-prices")
@RequiredArgsConstructor
@Slf4j
public class InvestmentPriceController {
    
    private final InvestmentPriceService investmentPriceService;
    private final YahooFinanceService yahooFinanceService;
    private final CryptoPriceService cryptoPriceService;
    private final InvestmentRepository investmentRepository;
    private final PortfolioRepository portfolioRepository;
    
    /**
     * Update current value for a specific investment
     * @param investmentId The investment ID
     * @return API response with update result
     */
    @PutMapping("/update/{investmentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> updateInvestmentValue(@PathVariable Long investmentId) {
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        
        if (investmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Investment investment = investmentOpt.get();
        
        boolean success = investmentPriceService.updateInvestmentCurrentValue(investment);
        
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, 
                "Investment value updated successfully", investment));
        } else {
            return ResponseEntity.ok(new ApiResponse(false,
                "Failed to update investment value", null));
        }
    }
    
    /**
     * Update all investments in a portfolio
     * @param portfolioId The portfolio ID
     * @return API response with update results
     */
    @PutMapping("/update/portfolio/{portfolioId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> updatePortfolioInvestments(@PathVariable Long portfolioId) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Portfolio portfolio = portfolioOpt.get();
        
        int updatedCount = investmentPriceService.updatePortfolioInvestments(portfolio);
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("updatedCount", updatedCount);
        
        return ResponseEntity.ok(new ApiResponse(true,
            String.format("Updated %d investments in portfolio", updatedCount),
            resultData));
    }
    
    /**
     * Update all active investments
     * @return API response with update results
     */
    @PutMapping("/update-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateAllInvestments() {
        investmentPriceService.scheduledInvestmentUpdate();
        return ResponseEntity.ok(new ApiResponse(true,
            "Investment update task scheduled", null));
    }
    
    /**
     * Get current price quotes for a list of ticker symbols
     * @param tickers Comma-separated list of ticker symbols
     * @return API response with price quotes
     */
    @GetMapping("/quotes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> getQuotes(@RequestParam String tickers) {
        List<String> tickerList = List.of(tickers.split(","))
            .stream()
            .map(String::trim)
            .collect(Collectors.toList());
        
        Map<String, YahooFinanceQuote> quotes = yahooFinanceService.batchFetchPrices(tickerList);
        
        return ResponseEntity.ok(new ApiResponse(true,
            String.format("Retrieved quotes for %d out of %d tickers", 
                quotes.size(), tickerList.size()),
            quotes));
    }
} 