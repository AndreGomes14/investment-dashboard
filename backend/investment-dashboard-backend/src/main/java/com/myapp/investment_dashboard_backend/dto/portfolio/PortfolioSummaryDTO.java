package com.myapp.investment_dashboard_backend.dto.portfolio;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentPerformanceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryDTO {
    private UUID portfolioId;
    private String portfolioName;
    private BigDecimal totalValue;
    private BigDecimal totalPerformance;
    private int activeInvestmentsCount;
    private InvestmentPerformanceDTO bestPerformer;
    private InvestmentPerformanceDTO worstPerformer;
}