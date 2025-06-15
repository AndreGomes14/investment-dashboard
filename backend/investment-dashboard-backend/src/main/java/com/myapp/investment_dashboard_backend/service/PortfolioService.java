package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.portfolio.HistoricalDataPointDTO;
import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioSummaryResponse;
import com.myapp.investment_dashboard_backend.dto.portfolio.UpdatePortfolioRequest;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioService {

    Optional<Portfolio> getPortfolioByIdWithInvestments(UUID id);

    Portfolio createPortfolio(String portfolioName);

    List<Portfolio> getPortfoliosByCurrentUser();

    Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioRequest request);

    BigDecimal calculatePortfolioValueInPreferredCurrency(Portfolio portfolio, User user);

    Portfolio updatePortfolioValues(UUID portfolioId);

    void deletePortfolio(UUID portfolioId);

    PortfolioSummaryResponse getOverallSummary();

    PortfolioSummaryResponse getPortfolioSummary(UUID portfolioId);

    void updateAllActiveInvestmentValuesForUser(User user);

    void updateAllActiveInvestmentValuesForCurrentUser();

    void scheduledUpdateAllUsersPortfolioValues();

    List<HistoricalDataPointDTO> getOverallUserValueHistory(String range);
} 