package com.myapp.investment_dashboard_backend.dto.portfolio;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentPerformanceDTO;
import com.myapp.investment_dashboard_backend.model.Investment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {
    private SummaryMetrics summary;
    private List<Investment> activeInvestments;
    private List<Investment> soldInvestments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryMetrics {
        private String portfolioName;
        private BigDecimal totalValue;
        private BigDecimal totalCostBasis;
        private BigDecimal unrealizedPnlAbsolute;
        private BigDecimal unrealizedPnlPercentage;
        private BigDecimal realizedPnlAbsolute;
        private Map<String, BigDecimal> assetAllocationByValue;
        private Map<String, BigDecimal> currencyAllocationByValue;

        private int activeInvestmentsCount;
        private InvestmentPerformanceDTO bestPerformer;
        private InvestmentPerformanceDTO worstPerformer;
    }
}