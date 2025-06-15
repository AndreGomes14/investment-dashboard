package com.myapp.investment_dashboard_backend.dto.portfolio;

public record PortfolioUpdateResults(
    int updatedInvestmentsCount,
    boolean wasPortfolioSaved
) {
} 