package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.investment.CreateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.investment.UpdateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.investment.SellInvestmentRequest;
import com.myapp.investment_dashboard_backend.model.Investment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentService {
    Optional<Investment> getInvestmentById(UUID id);
    Investment createInvestment(UUID portfolioId, CreateInvestmentRequest request);
    Investment updateInvestment(UUID id, UpdateInvestmentRequest request);
    boolean deleteInvestment(UUID id);
    List<Investment> getAllInvestments();
    List<Investment> getInvestmentsByPortfolioId(UUID portfolioId);
    BigDecimal getInvestmentCurrentValue(UUID investmentId);
    Investment updateInvestmentValue(UUID id);
    Investment sellInvestment(UUID id, SellInvestmentRequest request);
    // Assuming MarketDataService is an interface, it should be defined in this package too
    // or its definition confirmed if it's from a third party or another module.
} 