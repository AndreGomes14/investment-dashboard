package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.investment.CreateInvestmentRequest;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class CreateInvestmentMapper {

    protected PortfolioRepository portfolioRepository;

    @Autowired
    public void setPortfolioRepository(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "portfolio", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "lastUpdateDate", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Investment toEntity(CreateInvestmentRequest request);

    @AfterMapping
    protected void mapPortfolio(CreateInvestmentRequest request, @MappingTarget Investment entity) {
        if (request.getPortfolioId() != null) {
            Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                    .orElseThrow(() -> new RuntimeException("Portfolio not found with ID: " + request.getPortfolioId()));
            entity.setPortfolio(portfolio);
        }
    }

    @AfterMapping
    protected void initializeValues(@MappingTarget Investment entity) {
        if (entity.getCurrentValue() == null && entity.getPurchasePrice() != null) {
            entity.setCurrentValue(entity.getPurchasePrice());
        }

        entity.setLastUpdateDate(LocalDateTime.now());
    }
}