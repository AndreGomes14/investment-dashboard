package com.myapp.investment_dashboard_backend.dto.financial_planning;

import com.myapp.investment_dashboard_backend.utils.GoalStatus;
import com.myapp.investment_dashboard_backend.utils.GoalType;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class UpdateFinancialGoalRequest {
    @NotBlank(message = "Goal name is required")
    @Size(max = 100, message = "Goal name must not exceed 100 characters")
    private String goalName;

    @Size(max = 500, message = "Goal description must not exceed 500 characters")
    private String goalDescription;

    @NotNull(message = "Goal type is required")
    private GoalType goalType;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
    private BigDecimal targetAmount;

    @NotNull(message = "Current saved amount is required")
    @DecimalMin(value = "0.0", message = "Current saved amount must be greater than or equal to 0")
    private BigDecimal currentSaved;

    @NotNull(message = "Target date is required")
    private LocalDate targetDate;

    @NotNull(message = "Status is required")
    private GoalStatus status;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority;
}