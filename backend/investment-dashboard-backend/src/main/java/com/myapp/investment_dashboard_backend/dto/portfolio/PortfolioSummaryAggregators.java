package com.myapp.investment_dashboard_backend.dto.portfolio;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentPerformanceDTO;

import java.math.BigDecimal;

public class PortfolioSummaryAggregators {
    public BigDecimal totalValue = BigDecimal.ZERO;
    public BigDecimal totalCostBasis = BigDecimal.ZERO;
    public InvestmentPerformanceDTO bestPerformer = null;
    public InvestmentPerformanceDTO worstPerformer = null;
    public BigDecimal maxPnlPercent = new BigDecimal(Integer.MIN_VALUE);
    public BigDecimal minPnlPercent = new BigDecimal(Integer.MAX_VALUE);

    // Constructor can be added if specific initialization beyond defaults is needed,
    // or if immutability is desired (then fields would be final and set in constructor).
    // For this mutable aggregator pattern, public fields are common, or getters/setters.
} 