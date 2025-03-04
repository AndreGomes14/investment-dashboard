package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentDTO;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class InvestmentMapper {

    protected PortfolioRepository portfolioRepository;

    @Mapping(source = "portfolio.id", target = "portfolioId")
    @Mapping(target = "performance", ignore = true)
    public abstract InvestmentDTO toDto(Investment investment);

    public abstract List<InvestmentDTO> toDtoList(List<Investment> investments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "portfolio", ignore = true)
    public abstract Investment toEntity(InvestmentDTO investmentDTO);

    @AfterMapping
    protected void calculatePerformance(Investment investment, @MappingTarget InvestmentDTO dto) {
        if (investment.getPurchasePrice() != null &&
                investment.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0 &&
                investment.getCurrentValue() != null) {

            BigDecimal performance = investment.getCurrentValue()
                    .subtract(investment.getPurchasePrice())
                    .divide(investment.getPurchasePrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            dto.setPerformance(performance);
        } else {
            dto.setPerformance(BigDecimal.ZERO);
        }
    }

    @AfterMapping
    protected void mapPortfolio(InvestmentDTO dto, @MappingTarget Investment entity) {
        if (dto.getPortfolioId() != null) {
            Portfolio portfolio = portfolioRepository.findById(dto.getPortfolioId())
                    .orElseThrow(() -> new RuntimeException("Portfolio not found with ID: " + dto.getPortfolioId()));
            entity.setPortfolio(portfolio);
        }
    }
}
