package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.dto.portfolio.CreatePortfolioRequest;
import com.myapp.investment_dashboard_backend.dto.portfolio.UpdatePortfolioRequest;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
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
     * @param request DTO containing portfolio creation details (e.g., name).
     * @return ResponseEntity containing the created portfolio and HTTP status.
     */
    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody CreatePortfolioRequest request) {
        Portfolio createdPortfolio = portfolioService.createPortfolio(request.getName());
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
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Updates an existing portfolio's name and description.
     *
     * @param id      The ID of the portfolio to update.
     * @param request The DTO containing the updated details.
     * @return ResponseEntity containing the updated portfolio and HTTP status.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Portfolio> updatePortfolio(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePortfolioRequest request) {

        return ResponseEntity.ok(portfolioService.updatePortfolio(id, request));
    }

    /**
     * Calculates the current total value of a portfolio in the owner's preferred currency.
     *
     * @param id The ID of the portfolio to calculate the value for.
     * @return ResponseEntity containing the total value (BigDecimal) in the preferred currency,
     *         or relevant error status (404, 403, 500).
     */
    @GetMapping("/{id}/value")
    public ResponseEntity<BigDecimal> getPortfolioValueInPreferredCurrency(@PathVariable UUID id) {
        Optional<Portfolio> portfolioOptional = portfolioService.getPortfolioByIdWithInvestments(id);

        if (portfolioOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Portfolio portfolio = portfolioOptional.get();
        User owner = portfolio.getUser();

        if (owner == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Portfolio owner information missing.");
        }

        BigDecimal totalValue = portfolioService.calculatePortfolioValueInPreferredCurrency(portfolio, owner);

        if (totalValue != null) {
            return ResponseEntity.ok(totalValue);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves all portfolios for the currently authenticated user.
     *
     * @return ResponseEntity containing a list of portfolios and HTTP status.
     */
    @GetMapping
    public ResponseEntity<List<Portfolio>> getCurrentUserPortfolios() {
        List<Portfolio> portfolios = portfolioService.getPortfoliosByCurrentUser();
        return ResponseEntity.ok(portfolios);
    }

    /**
     * Deletes a portfolio by its ID.
     *
     * @param id The ID of the portfolio to delete.
     * @return ResponseEntity with HTTP status (e.g., 204 No Content, 404 Not Found, 403 Forbidden).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable UUID id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }
}

