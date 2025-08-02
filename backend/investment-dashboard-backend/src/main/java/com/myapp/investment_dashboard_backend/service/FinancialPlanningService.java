package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.financial_planning.*;
import com.myapp.investment_dashboard_backend.model.FinancialGoal;
import com.myapp.investment_dashboard_backend.model.FinancialProfile;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.repository.FinancialGoalRepository;
import com.myapp.investment_dashboard_backend.repository.FinancialProfileRepository;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import com.myapp.investment_dashboard_backend.utils.GoalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinancialPlanningService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialPlanningService.class);

    private final FinancialProfileRepository financialProfileRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final UserRepository userRepository;

    @Autowired
    public FinancialPlanningService(FinancialProfileRepository financialProfileRepository,
                                  FinancialGoalRepository financialGoalRepository,
                                  UserRepository userRepository) {
        this.financialProfileRepository = financialProfileRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.userRepository = userRepository;
    }

    public FinancialProfileDTO createOrUpdateFinancialProfile(UUID userId, CreateFinancialProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElse(new FinancialProfile());

        if (profile.getId() == null) {
            profile.setUser(user);
            logger.info("Creating new financial profile for user: {}", userId);
        } else {
            logger.info("Updating existing financial profile for user: {}", userId);
        }

        profile.setMonthlyIncome(request.getMonthlyIncome());
        profile.setMonthlyExpenses(request.getMonthlyExpenses());
        profile.setCurrency(request.getCurrency().toUpperCase());

        FinancialProfile savedProfile = financialProfileRepository.save(profile);
        return convertToDTO(savedProfile);
    }

    public FinancialProfileDTO updateFinancialProfile(UUID userId, UpdateFinancialProfileRequest request) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found"));

        profile.setMonthlyIncome(request.getMonthlyIncome());
        profile.setMonthlyExpenses(request.getMonthlyExpenses());

        FinancialProfile savedProfile = financialProfileRepository.save(profile);
        logger.info("Updated financial profile for user: {}", userId);
        return convertToDTO(savedProfile);
    }

    @Transactional(readOnly = true)
    public FinancialProfileDTO getFinancialProfile(UUID userId) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found"));
        return convertToDTO(profile);
    }

    public FinancialGoalDTO createFinancialGoal(UUID userId, CreateFinancialGoalRequest request) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found. Please create a financial profile first."));

        FinancialGoal goal = new FinancialGoal();
        goal.setFinancialProfile(profile);
        goal.setGoalName(request.getGoalName());
        goal.setGoalDescription(request.getGoalDescription());
        goal.setGoalType(request.getGoalType());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentSaved(request.getCurrentSaved() != null ? request.getCurrentSaved() : BigDecimal.ZERO);
        goal.setTargetDate(request.getTargetDate());
        goal.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        goal.setStatus(GoalStatus.ACTIVE);

        FinancialGoal savedGoal = financialGoalRepository.save(goal);
        logger.info("Created financial goal '{}' for user: {}", goal.getGoalName(), userId);
        return convertToGoalDTO(savedGoal, profile.getMonthlySavings());
    }

    public FinancialGoalDTO updateFinancialGoal(UUID userId, UUID goalId, UpdateFinancialGoalRequest request) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found"));

        FinancialGoal goal = financialGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Financial goal not found"));

        if (!goal.getFinancialProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Goal does not belong to this user");
        }

        goal.setGoalName(request.getGoalName());
        goal.setGoalDescription(request.getGoalDescription());
        goal.setGoalType(request.getGoalType());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentSaved(request.getCurrentSaved());
        goal.setTargetDate(request.getTargetDate());
        goal.setStatus(request.getStatus());
        goal.setPriority(request.getPriority());

        FinancialGoal savedGoal = financialGoalRepository.save(goal);
        logger.info("Updated financial goal '{}' for user: {}", goal.getGoalName(), userId);
        return convertToGoalDTO(savedGoal, profile.getMonthlySavings());
    }

    public void deleteFinancialGoal(UUID userId, UUID goalId) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found"));

        FinancialGoal goal = financialGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Financial goal not found"));

        if (!goal.getFinancialProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Goal does not belong to this user");
        }

        financialGoalRepository.delete(goal);
        logger.info("Deleted financial goal '{}' for user: {}", goal.getGoalName(), userId);
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalDTO> getFinancialGoals(UUID userId) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found"));

        List<FinancialGoal> goals = financialGoalRepository
                .findByFinancialProfileOrderByPriorityAscTargetDateAsc(profile);

        return goals.stream()
                .map(goal -> convertToGoalDTO(goal, profile.getMonthlySavings()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalDTO> getActiveFinancialGoals(UUID userId) {
        FinancialProfile profile = financialProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Financial profile not found"));

        List<FinancialGoal> activeGoals = financialGoalRepository
                .findByFinancialProfileAndStatus(profile, GoalStatus.ACTIVE);

        return activeGoals.stream()
                .map(goal -> convertToGoalDTO(goal, profile.getMonthlySavings()))
                .collect(Collectors.toList());
    }

    private FinancialProfileDTO convertToDTO(FinancialProfile profile) {
        FinancialProfileDTO dto = new FinancialProfileDTO();
        dto.setId(profile.getId());
        dto.setMonthlyIncome(profile.getMonthlyIncome());
        dto.setMonthlyExpenses(profile.getMonthlyExpenses());
        dto.setCurrency(profile.getCurrency());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setMonthlySavings(profile.getMonthlySavings());

        List<FinancialGoalDTO> goalDTOs = profile.getGoals().stream()
                .map(goal -> convertToGoalDTO(goal, profile.getMonthlySavings()))
                .collect(Collectors.toList());
        dto.setGoals(goalDTOs);

        return dto;
    }

    private FinancialGoalDTO convertToGoalDTO(FinancialGoal goal, BigDecimal monthlySavings) {
        FinancialGoalDTO dto = new FinancialGoalDTO();
        dto.setId(goal.getId());
        dto.setGoalName(goal.getGoalName());
        dto.setGoalDescription(goal.getGoalDescription());
        dto.setGoalType(goal.getGoalType());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentSaved(goal.getCurrentSaved());
        dto.setTargetDate(goal.getTargetDate());
        dto.setStatus(goal.getStatus());
        dto.setPriority(goal.getPriority());
        dto.setCreatedAt(goal.getCreatedAt());
        dto.setUpdatedAt(goal.getUpdatedAt());
        dto.setProgressPercentage(goal.getProgressPercentage());
        dto.setRemainingAmount(goal.getRemainingAmount());
        
        // Calculate months to target
        long monthsToTarget = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
        dto.setMonthsToTarget((int) monthsToTarget);
        
        // Calculate required monthly saving
        if (monthsToTarget > 0 && goal.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal requiredMonthlySaving = goal.getRemainingAmount()
                    .divide(BigDecimal.valueOf(monthsToTarget), 2, RoundingMode.HALF_UP);
            dto.setRequiredMonthlySaving(requiredMonthlySaving);
        } else {
            dto.setRequiredMonthlySaving(BigDecimal.ZERO);
        }

        return dto;
    }
}