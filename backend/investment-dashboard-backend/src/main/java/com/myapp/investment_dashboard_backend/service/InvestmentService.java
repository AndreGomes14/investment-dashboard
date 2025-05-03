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
     * Creates a new investment within a specific portfolio.
     * The portfolio must exist and belong to the authenticated user (logic should be added here or via security rules).
     *
     * @param portfolioId The ID of the portfolio to add the investment to.
     * @param request     The DTO containing the new investment details.
     * @return The created investment entity.
     * @throws ResourceNotFoundException if the portfolio is not found or accessible.
     */
    @Transactional
    public Investment createInvestment(UUID portfolioId, CreateInvestmentRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        // --- Mapping and Creation ---
        Investment investment = new Investment();
        investment.setPortfolio(portfolio); // Set from the found portfolio
        investment.setTicker(request.getTicker());
        investment.setCurrentValue(getInvestmentCurrentValue(investment.getId()));
        investment.setType(request.getType());
        investment.setAmount(request.getAmount());
        investment.setPurchasePrice(request.getPurchasePrice());
        investment.setCurrency(request.getCurrency().toUpperCase());
        investment.setStatus(StatusInvestment.ACTIVE);
        investment.setLastUpdateDate(LocalDateTime.now());

        logger.info("Attempting initial save for investment: Ticker={}, Type={}, PortfolioID={}",
                investment.getTicker(), investment.getType(), portfolio.getId());

        // --- Save the initial investment record ---
        Investment savedInvestment = investmentRepository.save(investment);
        logger.info("Successfully saved initial investment with ID: {}", savedInvestment.getId());

        // --- Attempt to fetch and set the current value immediately ---
        try {
            logger.info("Attempting to fetch current value for new investment: Ticker={}, Type={}, Currency={}",
                    savedInvestment.getTicker(), savedInvestment.getType(), savedInvestment.getCurrency());

            PriceInfo priceInfo = marketDataService.getCurrentValue(
                    savedInvestment.getTicker(),
                    savedInvestment.getType(),
                    savedInvestment.getCurrency()
            );

            if (priceInfo != null && priceInfo.value() != null) {
                logger.info("Current value fetched successfully: {}. Updating investment.", priceInfo.value());
                savedInvestment.setCurrentValue(priceInfo.value());
                savedInvestment.setLastUpdateDate(LocalDateTime.now()); // Ensure last update reflects price fetch
                // Save the investment again with the current value
                return investmentRepository.save(savedInvestment);
            } else {
                logger.warn("Failed to retrieve valid current value PriceInfo for new investment {} ({}). CurrentValue remains null.",
                        savedInvestment.getId(), savedInvestment.getTicker());
                // Return the investment without the current value as fetching failed
                return savedInvestment;
            }
        } catch (IOException e) {
            logger.error("IOException fetching current value immediately after creating investment {}: {}. CurrentValue remains null.",
                    savedInvestment.getId(), e.getMessage());
            return savedInvestment; // Return initially saved object on error
        } catch (Exception e) {
            logger.error("Unexpected error fetching current value immediately after creating investment {}: {}. CurrentValue remains null.",
                    savedInvestment.getId(), e.getMessage());
            return savedInvestment; // Return initially saved object on error
        }
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
     * Changed: Updates status to DELETED instead of hard delete.
     *
     * @param id The ID of the investment to mark as deleted.
     * @return true if the investment was found and updated, false otherwise.
     */
    @Transactional // Ensure this is transactional
    public boolean deleteInvestment(UUID id) {
        Optional<Investment> investmentOpt = investmentRepository.findById(id);
        if (investmentOpt.isPresent()) {
            Investment investment = investmentOpt.get();
            investment.setStatus(StatusInvestment.DELETED); // Set status
            investment.setLastUpdateDate(LocalDateTime.now()); // Update timestamp
            investmentRepository.save(investment); // Save the change
            logger.info("Marked investment {} as DELETED.", id);
            return true;
        } else {
            logger.warn("Investment {} not found for deletion (status update).", id);
            return false;
        }
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
     * Retrieves all investments belonging to a specific portfolio.
     * Also enforces authorization to ensure the current user owns the portfolio.
     *
     * @param portfolioId The ID of the portfolio.
     * @return A list of investments for that portfolio.
     * @throws ResourceNotFoundException if the portfolio is not found or not accessible by the current user.
     */
    public List<Investment> getInvestmentsByPortfolioId(UUID portfolioId) {

        // Simplified check (REMOVE or REPLACE with actual authorization):
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId);
        }

        // --- Fetch Investments ---
        logger.debug("Fetching investments for portfolio ID: {}", portfolioId);
        // Assuming Investment entity has a Portfolio field mapped correctly
        return investmentRepository.findByPortfolioId(portfolioId);
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

    /**
     * Marks an investment as SOLD.
     *
     * @param id The ID of the investment to mark as sold.
     * @return The updated investment object with SOLD status, or null if not found.
     */
    @Transactional
    public Investment sellInvestment(UUID id) {
        Optional<Investment> investmentOpt = investmentRepository.findById(id);
        if (investmentOpt.isPresent()) {
            Investment investment = investmentOpt.get();
            // Trim status from DB before comparing
            if (investment.getStatus() != null && StatusInvestment.ACTIVE.equals(investment.getStatus().trim())) {
                investment.setStatus(StatusInvestment.SOLD);
                investment.setLastUpdateDate(LocalDateTime.now());
                Investment soldInvestment = investmentRepository.save(investment);
                logger.info("Marked investment {} as SOLD.", id);
                return soldInvestment;
            } else {
                logger.warn("Attempted to sell investment {} which is not ACTIVE (Status: '{}'). Trimmed comparison failed.", id, investment.getStatus()); // Updated log message
                return investment; // Return unchanged investment
            }
        } else {
            logger.warn("Investment {} not found for selling.", id);
            throw new ResourceNotFoundException("Investment not found with id: " + id);
        }
    }
}