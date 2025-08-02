package com.myapp.investment_dashboard_backend.dto.financial_planning;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UpdateFinancialProfileRequest {
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Monthly expenses is required")
    @DecimalMin(value = "0.0", message = "Monthly expenses must be greater than or equal to 0")
    private BigDecimal monthlyExpenses;
}