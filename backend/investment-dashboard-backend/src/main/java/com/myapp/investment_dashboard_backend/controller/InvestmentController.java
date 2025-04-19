
package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.service.InvestmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    @Autowired
    public InvestmentController(InvestmentService investmentService) {
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
     * Creates a new investment.
     *
     * @param investment The investment object to create.
     * @return ResponseEntity containing the created investment.
     */
    @PostMapping
    public ResponseEntity<Investment> createInvestment(@RequestBody Investment investment) {
        Investment createdInvestment = investmentService.createInvestment(investment);
        return new ResponseEntity<>(createdInvestment, HttpStatus.CREATED);
    }

    /**
     * Updates an existing investment.
     *
     * @param id           The ID of the investment to update.
     * @param investment The updated investment object.
     * @return ResponseEntity containing the updated investment, or NOT_FOUND if the investment does not exist.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Investment> updateInvestment(@PathVariable UUID id, @RequestBody Investment investment) {
        Investment updatedInvestment = investmentService.updateInvestment(id, investment);
        if (updatedInvestment != null) {
            return new ResponseEntity<>(updatedInvestment, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
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

    /**
     * Retrieves the current value of an investment.
     *
     * @param id The ID of the investment.
     * @return ResponseEntity containing the current value, or NOT_FOUND if the investment does not exist, or INTERNAL_SERVER_ERROR if the value cannot be retrieved.
     */
    @GetMapping("/{id}/current-value")
    public ResponseEntity<BigDecimal> getInvestmentCurrentValue(@PathVariable UUID id) {
        BigDecimal currentValue = investmentService.getInvestmentCurrentValue(id);
        if (currentValue != null) {
            return new ResponseEntity<>(currentValue, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Or NOT_FOUND if investment doesn't exist
        }
    }

    /**
     * Updates the current value of an investment.
     *
     * @param id The ID of the investment to update.
     * @return ResponseEntity containing the updated investment, or NOT_FOUND if the investment does not exist, or INTERNAL_SERVER_ERROR if the update fails.
     */
    @PutMapping("/{id}/current-value")
    public ResponseEntity<Investment> updateInvestmentValue(@PathVariable UUID id) {
        Investment updatedInvestment = investmentService.updateInvestmentValue(id);
        if (updatedInvestment != null) {
            return new ResponseEntity<>(updatedInvestment, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); //  Or NOT_FOUND
        }
    }
}