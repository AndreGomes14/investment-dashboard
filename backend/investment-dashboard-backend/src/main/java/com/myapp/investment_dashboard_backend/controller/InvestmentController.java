package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.dto.investment.UpdateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.investment.SellInvestmentRequest;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
        return ResponseEntity.ok(investments);
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
        return investment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing investment.
     *
     * @param id      The ID of the investment to update.
     * @param request The DTO containing the fields to update.
     * @return ResponseEntity containing the updated investment.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Investment> updateInvestment(@PathVariable UUID id, @Valid @RequestBody UpdateInvestmentRequest request) {
        try {
            Investment updatedInvestment = investmentService.updateInvestment(id, request);
            return ResponseEntity.ok(updatedInvestment);
        } catch (Exception e) { // Catch potential ResourceNotFoundException from service
            // Consider more specific exception handling
            return ResponseEntity.notFound().build();
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
        boolean deleted = investmentService.deleteInvestment(id);
        if (deleted) {
            return ResponseEntity.ok().build(); // Return 200 OK if status updated
        } else {
            return ResponseEntity.notFound().build(); // Return 404 if not found
        }
    }

    /**
     * Marks an investment as sold.
     *
     * @param id The ID of the investment to mark as sold.
     * @param request DTO containing the sell price.
     * @return ResponseEntity containing the updated investment.
     */
    @PatchMapping("/{id}/sell")
    public ResponseEntity<Investment> sellInvestment(@PathVariable UUID id, @Valid @RequestBody SellInvestmentRequest request) {
        try {
            Investment soldInvestment = investmentService.sellInvestment(id, request);
            return ResponseEntity.ok(soldInvestment);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}