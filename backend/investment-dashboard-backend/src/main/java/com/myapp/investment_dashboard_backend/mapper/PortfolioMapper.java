package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioDTO;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        uses = {InvestmentMapper.class})
public abstract class PortfolioMapper {

    protected UserRepository userRepository;
    protected InvestmentMapper investmentMapper;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setInvestmentMapper(InvestmentMapper investmentMapper) {
        this.investmentMapper = investmentMapper;
    }

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "totalPerformance", ignore = true)
    public abstract PortfolioDTO toDto(Portfolio portfolio);

    @Named("toDtoWithoutInvestments")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "investments", ignore = true)
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "totalPerformance", ignore = true)
    public abstract PortfolioDTO toDtoWithoutInvestments(Portfolio portfolio);

    @IterableMapping(qualifiedByName = "toDtoWithoutInvestments")
    public abstract List<PortfolioDTO> toDtoList(List<Portfolio> portfolios);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "investments", ignore = true)
    public abstract Portfolio toEntity(PortfolioDTO portfolioDTO);

    @AfterMapping
    protected void calculatePortfolioMetrics(Portfolio portfolio, @MappingTarget PortfolioDTO dto) {
        if (portfolio.getInvestments() != null && !portfolio.getInvestments().isEmpty()) {
            dto.setInvestments(investmentMapper.toDtoList(portfolio.getInvestments()));

            BigDecimal totalValue = calculateTotalValue(portfolio);
            BigDecimal totalCost = calculateTotalCost(portfolio);

            dto.setTotalValue(totalValue);
            dto.setTotalPerformance(calculatePerformance(totalValue, totalCost));
        } else {
            dto.setTotalValue(BigDecimal.ZERO);
            dto.setTotalPerformance(BigDecimal.ZERO);
        }
    }

    private BigDecimal calculateTotalValue(Portfolio portfolio) {
        BigDecimal totalValue = BigDecimal.ZERO;

        for (var investment : portfolio.getInvestments()) {
            if (isActiveInvestment(investment)) {
                BigDecimal investmentValue = investment.getCurrentValue()
                        .multiply(investment.getAmount());
                totalValue = totalValue.add(investmentValue);
            }
        }

        return totalValue;
    }

    private BigDecimal calculateTotalCost(Portfolio portfolio) {
        BigDecimal totalCost = BigDecimal.ZERO;

        for (var investment : portfolio.getInvestments()) {
            if (isActiveInvestment(investment) && investment.getPurchasePrice() != null) {
                BigDecimal cost = investment.getPurchasePrice()
                        .multiply(investment.getAmount());
                totalCost = totalCost.add(cost);
            }
        }

        return totalCost;
    }

    private boolean isActiveInvestment(Investment investment) {
        return "ACTIVE".equals(investment.getStatus()) &&
                investment.getCurrentValue() != null &&
                investment.getAmount() != null;
    }

    private BigDecimal calculatePerformance(BigDecimal totalValue, BigDecimal totalCost) {
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return totalValue.subtract(totalCost)
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            return BigDecimal.ZERO;
        }
    }

    @AfterMapping
    protected void mapUser(PortfolioDTO dto, @MappingTarget Portfolio entity) {
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + dto.getUserId()));
            entity.setUser(user);
        }
    }
}