package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentPerformanceDTO;
import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioSummaryDTO;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Optional;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class PortfolioSummaryMapper {

    private static final String ACTIVE_STATUS = "ACTIVE";

    @Mapping(source = "id", target = "portfolioId")
    @Mapping(source = "name", target = "portfolioName")
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "totalPerformance", ignore = true)
    @Mapping(target = "activeInvestmentsCount", ignore = true)
    @Mapping(target = "bestPerformer", ignore = true)
    @Mapping(target = "worstPerformer", ignore = true)
    public abstract PortfolioSummaryDTO toDto(Portfolio portfolio);

    @AfterMapping
    protected void calculateSummary(Portfolio portfolio, @MappingTarget PortfolioSummaryDTO dto) {
        if (portfolio.getInvestments() == null || portfolio.getInvestments().isEmpty()) {
            dto.setTotalValue(BigDecimal.ZERO);
            dto.setTotalPerformance(BigDecimal.ZERO);
            dto.setActiveInvestmentsCount(0);
            return;
        }

        long activeCount = portfolio.getInvestments().stream()
                .filter(i -> ACTIVE_STATUS.equals(i.getStatus()))
                .count();
        dto.setActiveInvestmentsCount((int) activeCount);

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Investment investment : portfolio.getInvestments()) {
            if (ACTIVE_STATUS.equals(investment.getStatus()) &&
                    investment.getCurrentValue() != null &&
                    investment.getAmount() != null) {

                BigDecimal investmentValue = investment.getCurrentValue()
                        .multiply(investment.getAmount());
                totalValue = totalValue.add(investmentValue);

                if (investment.getPurchasePrice() != null) {
                    BigDecimal cost = investment.getPurchasePrice()
                            .multiply(investment.getAmount());
                    totalCost = totalCost.add(cost);
                }
            }
        }

        dto.setTotalValue(totalValue);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            dto.setTotalPerformance(totalValue.subtract(totalCost)
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            dto.setTotalPerformance(BigDecimal.ZERO);
        }

        Optional<Investment> bestPerformer = portfolio.getInvestments().stream()
                .filter(i -> ACTIVE_STATUS.equals(i.getStatus()))
                .filter(i -> i.getPurchasePrice() != null && i.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0
                        && i.getCurrentValue() != null)
                .max(Comparator.comparing(this::calculatePerformance));

        Optional<Investment> worstPerformer = portfolio.getInvestments().stream()
                .filter(i -> ACTIVE_STATUS.equals(i.getStatus()))
                .filter(i -> i.getPurchasePrice() != null && i.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0
                        && i.getCurrentValue() != null)
                .min(Comparator.comparing(this::calculatePerformance));

        bestPerformer.ifPresent(investment ->
                dto.setBestPerformer(InvestmentPerformanceDTO.builder()
                        .id(investment.getId())
                        .ticker(investment.getTicker())
                        .type(investment.getType())
                        .performance(calculatePerformance(investment))
                        .build())
        );

        worstPerformer.ifPresent(investment ->
                dto.setWorstPerformer(InvestmentPerformanceDTO.builder()
                        .id(investment.getId())
                        .ticker(investment.getTicker())
                        .type(investment.getType())
                        .performance(calculatePerformance(investment))
                        .build())
        );
    }

    private BigDecimal calculatePerformance(Investment investment) {
        if (investment.getPurchasePrice() == null || investment.getPurchasePrice().compareTo(BigDecimal.ZERO) == 0
                || investment.getCurrentValue() == null) {
            return BigDecimal.ZERO;
        }

        return investment.getCurrentValue().subtract(investment.getPurchasePrice())
                .divide(investment.getPurchasePrice(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}