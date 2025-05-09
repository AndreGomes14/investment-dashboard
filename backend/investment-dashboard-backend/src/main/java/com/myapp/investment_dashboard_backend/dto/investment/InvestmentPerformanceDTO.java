package com.myapp.investment_dashboard_backend.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentPerformanceDTO {
    private String ticker;
    private String type;
    private BigDecimal unrealizedPnlPercentage;
    private BigDecimal currentValue; // Current value per unit
    private BigDecimal holdingValue; // Total current value of the holding
}