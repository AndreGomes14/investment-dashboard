package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.dto.financial_planning.*;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import com.myapp.investment_dashboard_backend.service.FinancialPlanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial-planning")
@RequiredArgsConstructor
public class FinancialPlanningController {

    private final FinancialPlanningService financialPlanningService;
    private final UserRepository userRepository;

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
        return user.getId();
    }

    @PostMapping("/profile")
    public ResponseEntity<FinancialProfileDTO> createOrUpdateFinancialProfile(
            @Valid @RequestBody CreateFinancialProfileRequest request) {
        UUID userId = getCurrentUserId();
        FinancialProfileDTO profile = financialPlanningService.createOrUpdateFinancialProfile(userId, request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<FinancialProfileDTO> updateFinancialProfile(
            @Valid @RequestBody UpdateFinancialProfileRequest request) {
        UUID userId = getCurrentUserId();
        FinancialProfileDTO profile = financialPlanningService.updateFinancialProfile(userId, request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile")
    public ResponseEntity<FinancialProfileDTO> getFinancialProfile() {
        try {
            UUID userId = getCurrentUserId();
            FinancialProfileDTO profile = financialPlanningService.getFinancialProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Financial profile not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @PostMapping("/goals")
    public ResponseEntity<FinancialGoalDTO> createFinancialGoal(
            @Valid @RequestBody CreateFinancialGoalRequest request) {
        UUID userId = getCurrentUserId();
        FinancialGoalDTO goal = financialPlanningService.createFinancialGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(goal);
    }

    @PutMapping("/goals/{goalId}")
    public ResponseEntity<FinancialGoalDTO> updateFinancialGoal(
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateFinancialGoalRequest request) {
        UUID userId = getCurrentUserId();
        FinancialGoalDTO goal = financialPlanningService.updateFinancialGoal(userId, goalId, request);
        return ResponseEntity.ok(goal);
    }

    @DeleteMapping("/goals/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFinancialGoal(@PathVariable UUID goalId) {
        UUID userId = getCurrentUserId();
        financialPlanningService.deleteFinancialGoal(userId, goalId);
    }

    @GetMapping("/goals")
    public ResponseEntity<List<FinancialGoalDTO>> getFinancialGoals() {
        try {
            UUID userId = getCurrentUserId();
            List<FinancialGoalDTO> goals = financialPlanningService.getFinancialGoals(userId);
            return ResponseEntity.ok(goals);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Financial profile not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @GetMapping("/goals/active")
    public ResponseEntity<List<FinancialGoalDTO>> getActiveFinancialGoals() {
        try {
            UUID userId = getCurrentUserId();
            List<FinancialGoalDTO> activeGoals = financialPlanningService.getActiveFinancialGoals(userId);
            return ResponseEntity.ok(activeGoals);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Financial profile not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}