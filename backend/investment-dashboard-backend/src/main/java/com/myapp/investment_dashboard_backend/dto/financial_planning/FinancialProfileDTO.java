package com.myapp.investment_dashboard_backend.dto.financial_planning;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FinancialProfileDTO {
    private UUID id;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private String currency;
    private List<FinancialGoalDTO> goals;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal monthlySavings;
}