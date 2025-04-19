package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * Retrieves a portfolio by its ID, including its investments.
     *
     * @param id The ID of the portfolio to retrieve.
     * @return ResponseEntity containing the portfolio and HTTP status.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable UUID id) {
        Optional<Portfolio> portfolio = portfolioService.getPortfolioByIdWithInvestments(id);
        return portfolio.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    /**
     * Creates a new portfolio.
     *
     * @param portfolio The portfolio object to create.
     * @return ResponseEntity containing the created portfolio and HTTP status.
     */

    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody Portfolio portfolio) {
        Portfolio createdPortfolio = portfolioService.createPortfolio(portfolio);
        return new ResponseEntity<>(createdPortfolio, HttpStatus.CREATED);
    }

    /**
     * Updates the values of all investments in a portfolio and recalculates the portfolio value.
     *
     * @param id The ID of the portfolio to update.
     * @return ResponseEntity containing the updated portfolio and HTTP status.
     */
    @PostMapping("/{id}/update-values")
    public ResponseEntity<Portfolio> updatePortfolioValues(@PathVariable UUID id) {
        Portfolio updatedPortfolio = portfolioService.updatePortfolioValues(id);
        if (updatedPortfolio != null) {
            return new ResponseEntity<>(updatedPortfolio, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Or HttpStatus.NOT_FOUND if portfolio not found
        }
    }

    /**
     * Calculates the current total value of a portfolio.
     *
     * @param id The ID of the portfolio to calculate the value for.
     * @return ResponseEntity containing the total value and HTTP status.
     */
    @GetMapping("/{id}/value")
    public ResponseEntity<BigDecimal> getPortfolioValue(@PathVariable UUID id) {
        Optional<Portfolio> portfolioOptional = portfolioService.getPortfolioByIdWithInvestments(id);
        if (portfolioOptional.isPresent()) {
            Portfolio portfolio = portfolioOptional.get();
            BigDecimal totalValue = portfolioService.calculatePortfolioValue(portfolio);
            if (totalValue != null) {
                return new ResponseEntity<>(totalValue, HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}

