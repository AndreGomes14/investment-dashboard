package com.myapp.investment_dashboard_backend.dto.financial_planning;

import com.myapp.investment_dashboard_backend.utils.GoalStatus;
import com.myapp.investment_dashboard_backend.utils.GoalType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FinancialGoalDTO {
    private UUID id;
    private String goalName;
    private String goalDescription;
    private GoalType goalType;
    private BigDecimal targetAmount;
    private BigDecimal currentSaved;
    private LocalDate targetDate;
    private GoalStatus status;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal progressPercentage;
    private BigDecimal remainingAmount;
    private Integer monthsToTarget;
    private BigDecimal requiredMonthlySaving;
}