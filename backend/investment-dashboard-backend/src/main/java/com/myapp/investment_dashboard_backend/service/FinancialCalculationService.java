package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.financial_planning.FinancialGoalDTO;
import com.myapp.investment_dashboard_backend.dto.financial_planning.FinancialProfileDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialCalculationService {

    public Map<String, Object> calculateFinancialInsights(FinancialProfileDTO profile, List<FinancialGoalDTO> goals) {
        Map<String, Object> insights = new HashMap<>();
        
        BigDecimal monthlySavings = profile.getMonthlySavings();
        insights.put("monthlySavings", monthlySavings);
        insights.put("savingsRate", calculateSavingsRate(profile));
        insights.put("totalGoalsValue", calculateTotalGoalsValue(goals));
        insights.put("totalRequiredMonthlySaving", calculateTotalRequiredMonthlySaving(goals));
        insights.put("feasibilityAnalysis", analyzeFeasibility(monthlySavings, goals));
        insights.put("goalsPriorityRecommendation", prioritizeGoals(monthlySavings, goals));
        insights.put("emergencyFundRecommendation", calculateEmergencyFundRecommendation(profile));
        
        return insights;
    }

    public BigDecimal calculateSavingsRate(FinancialProfileDTO profile) {
        if (profile.getMonthlyIncome().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profile.getMonthlySavings()
                .divide(profile.getMonthlyIncome(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal calculateTotalGoalsValue(List<FinancialGoalDTO> goals) {
        return goals.stream()
                .map(FinancialGoalDTO::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalRequiredMonthlySaving(List<FinancialGoalDTO> goals) {
        return goals.stream()
                .filter(goal -> goal.getRequiredMonthlySaving() != null)
                .map(FinancialGoalDTO::getRequiredMonthlySaving)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> analyzeFeasibility(BigDecimal monthlySavings, List<FinancialGoalDTO> goals) {
        Map<String, Object> analysis = new HashMap<>();
        
        BigDecimal totalRequired = calculateTotalRequiredMonthlySaving(goals);
        boolean isFeasible = monthlySavings.compareTo(totalRequired) >= 0;
        
        analysis.put("isFeasible", isFeasible);
        analysis.put("totalRequiredSaving", totalRequired);
        analysis.put("availableSavings", monthlySavings);
        analysis.put("surplus", monthlySavings.subtract(totalRequired));
        
        if (!isFeasible) {
            BigDecimal deficit = totalRequired.subtract(monthlySavings);
            analysis.put("deficit", deficit);
            analysis.put("recommendation", "Consider increasing income, reducing expenses, or adjusting goal timelines");
        }
        
        return analysis;
    }

    public List<String> prioritizeGoals(BigDecimal monthlySavings, List<FinancialGoalDTO> goals) {
        // Sort goals by priority and urgency
        return goals.stream()
                .sorted((g1, g2) -> {
                    // First by priority (lower number = higher priority)
                    int priorityCompare = g1.getPriority().compareTo(g2.getPriority());
                    if (priorityCompare != 0) return priorityCompare;
                    
                    // Then by urgency (closer target date = higher priority)
                    return g1.getTargetDate().compareTo(g2.getTargetDate());
                })
                .map(goal -> String.format("%s (Priority: %d, Due: %s, Required: %s/month)", 
                        goal.getGoalName(), 
                        goal.getPriority(), 
                        goal.getTargetDate(),
                        goal.getRequiredMonthlySaving()))
                .toList();
    }

    public Map<String, Object> calculateEmergencyFundRecommendation(FinancialProfileDTO profile) {
        Map<String, Object> recommendation = new HashMap<>();
        
        // Standard recommendation: 3-6 months of expenses
        BigDecimal threeMonthsExpenses = profile.getMonthlyExpenses().multiply(BigDecimal.valueOf(3));
        BigDecimal sixMonthsExpenses = profile.getMonthlyExpenses().multiply(BigDecimal.valueOf(6));
        
        recommendation.put("minimumRecommended", threeMonthsExpenses);
        recommendation.put("idealAmount", sixMonthsExpenses);
        recommendation.put("monthsOfExpensesCovered", calculateMonthsOfExpensesCovered(profile));
        
        return recommendation;
    }

    private BigDecimal calculateMonthsOfExpensesCovered(FinancialProfileDTO profile) {
        if (profile.getMonthlyExpenses().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // This would ideally check current savings/emergency fund, but for now we'll use available monthly savings
        BigDecimal currentSavings = profile.getMonthlySavings().multiply(BigDecimal.valueOf(12)); // Assume one year of savings
        return currentSavings.divide(profile.getMonthlyExpenses(), 2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> calculateGoalProjections(FinancialGoalDTO goal, BigDecimal monthlySavingAllocation) {
        Map<String, Object> projections = new HashMap<>();
        
        BigDecimal remainingAmount = goal.getRemainingAmount();
        
        if (monthlySavingAllocation.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate months needed with current allocation
            BigDecimal monthsNeeded = remainingAmount.divide(monthlySavingAllocation, 2, RoundingMode.HALF_UP);
            LocalDate projectedCompletionDate = LocalDate.now().plusMonths(monthsNeeded.longValue());
            
            projections.put("monthsNeeded", monthsNeeded);
            projections.put("projectedCompletionDate", projectedCompletionDate);
            projections.put("willMeetTargetDate", projectedCompletionDate.isBefore(goal.getTargetDate()) || 
                                                 projectedCompletionDate.equals(goal.getTargetDate()));
            
            // Calculate progress timeline
            LocalDate currentDate = LocalDate.now();
            long totalMonths = ChronoUnit.MONTHS.between(currentDate, goal.getTargetDate());
            BigDecimal progressPerMonth = monthlySavingAllocation.divide(remainingAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            projections.put("progressPerMonth", progressPerMonth);
            projections.put("timelineMonths", totalMonths);
        }
        
        return projections;
    }
}