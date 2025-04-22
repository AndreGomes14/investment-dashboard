package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.investment_dashboard_backend.dto.investment.CreateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.investment.UpdateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.exception.ResourceNotFoundException;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.utils.StatusInvestment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final MarketDataService marketDataService;
    private static final Logger logger = LoggerFactory.getLogger(InvestmentService.class);
    private final PortfolioRepository portfolioRepository;

    @Autowired
    public InvestmentService(InvestmentRepository investmentRepository, MarketDataService marketDataService, PortfolioRepository portfolioRepository) {
        this.investmentRepository = investmentRepository;
        this.marketDataService = marketDataService;
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * Retrieves an investment by its ID.
     *
     * @param id The ID of the investment to retrieve.
     * @return An Optional containing the investment if found.
     */
    public Optional<Investment> getInvestmentById(UUID id) {
        return investmentRepository.findById(id);
    }

    /**
     * Creates a new investment.
     */
    @Transactional
    public Investment createInvestment(CreateInvestmentRequest request) {
        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + request.getPortfolioId()));

        Investment investment = new Investment();
        investment.setPortfolio(portfolio);
        investment.setTicker(request.getTicker());
        investment.setType(request.getType());
        investment.setAmount(request.getAmount());
        investment.setPurchasePrice(request.getPurchasePrice());
        investment.setCurrency(request.getCurrency().toUpperCase());
        investment.setStatus(StatusInvestment.ACTIVE);
        investment.setLastUpdateDate(LocalDateTime.now());

        logger.info("Creating new investment: Ticker={}, Type={}, Amount={}, Price={}, Currency={}, PortfolioID={}",
                investment.getTicker(), investment.getType(), investment.getAmount(), investment.getPurchasePrice(), investment.getCurrency(), portfolio.getId());

        return investmentRepository.save(investment);
    }

    /**
     * Updates an existing investment based on data from a DTO.
     *
     * @param id      The ID of the investment to update.
     * @param request The DTO containing the update details.
     * @return The updated investment entity.
     * @throws ResourceNotFoundException if the investment with the given ID is not found.
     */
    @Transactional
    public Investment updateInvestment(UUID id, UpdateInvestmentRequest request) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + id));

        if (request.getTicker() != null) {
            investment.setTicker(request.getTicker());
        }
        if (request.getType() != null) {
            investment.setType(request.getType());
        }
        if (request.getAmount() != null) {
            investment.setAmount(request.getAmount());
        }
        if (request.getPurchasePrice() != null) {
            investment.setPurchasePrice(request.getPurchasePrice());
        }
        investment.setLastUpdateDate(LocalDateTime.now()); // Manually set for consistency if needed, though @UpdateTimestamp might suffice

        logger.info("Updating investment {}: Ticker={}, Type={}, Amount={}, Price={}",
                id, investment.getTicker(), investment.getType(), investment.getAmount(), investment.getPurchasePrice());

        return investmentRepository.save(investment);
    }

    /**
     * Deletes an investment by its ID.
     *
     * @param id The ID of the investment to delete.
     */
    public void deleteInvestment(UUID id) {
        investmentRepository.deleteById(id);
    }

    /**
     * Retrieves all investments.
     *
     * @return A list of all investments.
     */
    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    /**
     * Gets the current value of the investment by calling MarketDataService.
     */
    public BigDecimal getInvestmentCurrentValue(UUID investmentId) {
        Optional<Investment> investmentOptional = investmentRepository.findById(investmentId);
        if (investmentOptional.isPresent()) {
            Investment investment = investmentOptional.get();
            if (investment.getCurrency() == null || investment.getCurrency().trim().isEmpty()) {
                logger.warn("Investment {} is missing currency information. Cannot fetch current value.", investmentId);
                return null;
            }
            try {
                PriceInfo priceInfo = marketDataService.getCurrentValue(
                        investment.getTicker(),
                        investment.getType(),
                        investment.getCurrency()
                );
                // Return the value from PriceInfo, if available
                return (priceInfo != null) ? priceInfo.value() : null;
            } catch (IOException e) {
                logger.error("IOException fetching current value for investment {}: {}", investmentId, e.getMessage());
                return null;
            } catch (Exception e) {
                logger.error("Unexpected error fetching current value for investment {}: {}", investmentId, e.getMessage());
                return null;
            }
        } else {
            logger.warn("Attempted to get current value for non-existent investment: {}", investmentId);
            return null;
        }
    }

    /**
     * Updates the current value of an investment using MarketDataService.
     *
     * @param id The ID of the investment to update.
     * @return The updated investment, or null if the investment does not exist or the update fails.
     */
    @Transactional
    public Investment updateInvestmentValue(UUID id) {
        Optional<Investment> investmentOptional = investmentRepository.findById(id);
        if (investmentOptional.isPresent()) {
            Investment investment = investmentOptional.get();
            if (investment.getCurrency() == null || investment.getCurrency().trim().isEmpty()) {
                logger.warn("Investment {} is missing currency information. Cannot update current value.", id);
                return null;
            }
            try {
                PriceInfo priceInfo = marketDataService.getCurrentValue(
                        investment.getTicker(),
                        investment.getType(),
                        investment.getCurrency()
                );

                if (priceInfo != null && priceInfo.value() != null) {
                    investment.setCurrentValue(priceInfo.value());
                    investment.setLastUpdateDate(LocalDateTime.now());
                    return investmentRepository.save(investment);
                } else {
                    logger.warn("Failed to retrieve valid current value PriceInfo for investment {} ({}). Update aborted.", id, investment.getTicker());
                    return null;
                }
            } catch (IOException e) {
                logger.error("IOException updating current value for investment {}: {}", id, e.getMessage());
                return null;
            } catch (Exception e) {
                logger.error("Unexpected error updating current value for investment {}: {}", id, e.getMessage());
                return null;
            }
        } else {
            logger.warn("Attempted to update current value for non-existent investment: {}", id);
            return null;
        }
    }
}