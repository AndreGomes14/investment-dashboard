package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class CreateInvestmentMapper {

    protected PortfolioRepository portfolioRepository;

    @Autowired
    public void setPortfolioRepository(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

}