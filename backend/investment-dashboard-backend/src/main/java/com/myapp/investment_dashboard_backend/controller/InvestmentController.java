package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.service.InvestmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    /**
     * Retrieves all investments.
     *
     * @return ResponseEntity containing the list of investments.
     */
    @GetMapping
    public ResponseEntity<List<Investment>> getAllInvestments() {
        List<Investment> investments = investmentService.getAllInvestments();
        return new ResponseEntity<>(investments, HttpStatus.OK);
    }

    /**
     * Retrieves an investment by its ID.
     *
     * @param id The ID of the investment to retrieve.
     * @return ResponseEntity containing the investment, or NOT_FOUND if the investment does not exist.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Investment> getInvestmentById(@PathVariable UUID id) {
        Optional<Investment> investment = investmentService.getInvestmentById(id);
        return investment.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Deletes an investment by its ID.
     *
     * @param id The ID of the investment to delete.
     * @return ResponseEntity with NO_CONTENT status.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable UUID id) {
        investmentService.deleteInvestment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}