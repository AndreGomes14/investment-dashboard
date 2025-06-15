package com.myapp.investment_dashboard_backend.service.impl;

import com.myapp.investment_dashboard_backend.dto.investment.CreateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.investment.UpdateInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.investment.SellInvestmentRequest;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.exception.ResourceNotFoundException;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.service.InvestmentService;
import com.myapp.investment_dashboard_backend.service.MarketDataService;
import com.myapp.investment_dashboard_backend.service.PortfolioService;
import com.myapp.investment_dashboard_backend.utils.StatusInvestment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestmentServiceImpl implements InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final MarketDataServiceImpl marketDataService;
    private static final Logger logger = LoggerFactory.getLogger(InvestmentServiceImpl.class);
    private final PortfolioRepository portfolioRepository;
    private final PortfolioService portfolioService;

    @Autowired
    public InvestmentServiceImpl(InvestmentRepository investmentRepository, MarketDataServiceImpl marketDataService, PortfolioRepository portfolioRepository, PortfolioService portfolioService) {
        this.investmentRepository = investmentRepository;
        this.marketDataService = marketDataService;
        this.portfolioRepository = portfolioRepository;
        this.portfolioService = portfolioService;
    }

    @Override
    public Optional<Investment> getInvestmentById(UUID id) {
        return investmentRepository.findById(id);
    }

    @Override
    @Transactional
    public Investment createInvestment(UUID portfolioId, CreateInvestmentRequest request) {
        // Validate ticker after potential defaulting in controller
        if (request.getTicker() == null || request.getTicker().isBlank()) {
            // This case should ideally only be hit if type is not "Other" and ticker was blank,
            // as "Other" types should have had ticker defaulted by PortfolioController.
            throw new IllegalArgumentException("Ticker is required and was not provided or defaulted.");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        Investment investment = mapToNewInvestment(request, portfolio);

        logger.info("Attempting initial save for investment: Ticker={}, Type={}, PortfolioID={}",
                investment.getTicker(), investment.getType(), portfolio.getId());
        Investment savedInvestment = investmentRepository.save(investment);
        logger.info("Successfully saved initial investment with ID: {}", savedInvestment.getId());

        Investment finalInvestment = fetchAndSetInitialCurrentValue(savedInvestment);

        // Recalculate portfolio totals and record user history to keep statistics up to date after adding a new investment
        try {
            portfolioService.updatePortfolioValues(portfolioId);
        } catch (Exception e) {
            logger.error("Failed to recalculate portfolio {} values after creating investment {}: {}", portfolioId, finalInvestment.getId(), e.getMessage(), e);
        }

        return finalInvestment;
    }

    private Investment mapToNewInvestment(CreateInvestmentRequest request, Portfolio portfolio) {
        Investment investment = new Investment();
        investment.setPortfolio(portfolio);
        investment.setTicker(request.getTicker());
        investment.setType(request.getType());
        investment.setAmount(request.getAmount());
        investment.setPurchasePrice(request.getPurchasePrice());
        investment.setCurrency(request.getCurrency().toUpperCase());
        investment.setStatus(StatusInvestment.ACTIVE);
        investment.setSellPrice(request.getPurchasePrice());
        investment.setLastUpdateDate(LocalDateTime.now());
        // Set custom name if provided (used for 'Other' asset types or descriptive alias)
        if (request.getName() != null && !request.getName().isBlank()) {
            investment.setCustomName(request.getName());
        }
        // If the user supplied a current value (e.g., for 'Other' assets), persist it directly.
        if (request.getCurrentValue() != null) {
            investment.setCurrentValue(request.getCurrentValue());
        }
        return investment;
    }

    private Investment fetchAndSetInitialCurrentValue(Investment investment) {
        // If current value already set (e.g., for "Other" type entered by the user), just persist and return.
        if (investment.getCurrentValue() != null) {
            return investmentRepository.save(investment);
        }

        if (!isInvestmentCurrencyValid(investment)) {
            logger.warn("Cannot fetch initial current value for investment {} due to invalid currency.", investment.getId());
            return investment;
        }
        try {
            logger.info("Attempting to fetch current value for new investment: Ticker={}, Type={}, Currency={}",
                    investment.getTicker(), investment.getType(), investment.getCurrency());

            PriceInfo priceInfo = fetchPriceInfoInternal(investment);

            if (priceInfo != null && priceInfo.value() != null) {
                logger.info("Current value fetched successfully: {}. Updating investment.", priceInfo.value());
                investment.setCurrentValue(priceInfo.value());
                investment.setLastUpdateDate(LocalDateTime.now());
                return investmentRepository.save(investment);
            } else {
                logger.warn("Failed to retrieve valid current value PriceInfo for new investment {} ({}). CurrentValue remains null.",
                        investment.getId(), investment.getTicker());
                return investment;
            }
        } catch (IOException e) {
            logger.error("IOException fetching current value immediately after creating investment {}: {}. CurrentValue remains null.",
                    investment.getId(), e.getMessage());
            return investment;
        } catch (Exception e) {
            logger.error("Unexpected error fetching current value immediately after creating investment {}: {}. CurrentValue remains null.",
                    investment.getId(), e.getMessage(), e);
            return investment;
        }
    }

    private boolean isInvestmentCurrencyValid(Investment investment) {
        if (investment.getCurrency() == null || investment.getCurrency().trim().isEmpty()) {
            logger.warn("Investment {} is missing currency information. Cannot process market data.", investment.getId());
            return false;
        }
        return true;
    }

    private PriceInfo fetchPriceInfoInternal(Investment investment) throws IOException {
        logger.debug("Fetching price info for investment: Ticker={}, Type={}, Currency={}",
                investment.getTicker(), investment.getType(), investment.getCurrency());
        return marketDataService.getCurrentValue(
                investment.getTicker(),
                investment.getType(),
                investment.getCurrency()
        );
    }

    @Override
    @Transactional
    public Investment updateInvestment(UUID id, UpdateInvestmentRequest request) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + id));

        applyUpdateRequest(investment, request);

        logger.info("Updating investment {}: Ticker={}, Type={}, Amount={}, Price={}",
                id, investment.getTicker(), investment.getType(), investment.getAmount(), investment.getPurchasePrice());

        return investmentRepository.save(investment);
    }

    private void applyUpdateRequest(Investment investment, UpdateInvestmentRequest request) {
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
        if (request.getCurrentValue() != null) {
            investment.setCurrentValue(request.getCurrentValue());
        }
        investment.setLastUpdateDate(LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean deleteInvestment(UUID id) {
        return investmentRepository.findById(id)
                .map(investment -> {
                    investment.setStatus(StatusInvestment.DELETED);
                    investment.setLastUpdateDate(LocalDateTime.now());
                    investmentRepository.save(investment);
                    logger.info("Marked investment {} as DELETED.", id);
                    return true;
                })
                .orElseGet(() -> {
                    logger.warn("Investment {} not found for deletion (status update).", id);
                    return false;
                });
    }

    @Override
    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    @Override
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

    @Override
    public BigDecimal getInvestmentCurrentValue(UUID investmentId) {
        return investmentRepository.findById(investmentId)
                .filter(this::isInvestmentCurrencyValid)
                .flatMap(this::tryFetchPriceInfo)
                .map(PriceInfo::value)
                .orElseGet(() -> {
                    if (!investmentRepository.existsById(investmentId)) {
                         logger.warn("Attempted to get current value for non-existent investment: {}", investmentId);
                    }
                    // Specific logs for currency invalid or fetch failure are in their respective methods.
                    return null;
                });
    }

    @Override
    @Transactional
    public Investment updateInvestmentValue(UUID id) {
        return investmentRepository.findById(id)
                .filter(this::isInvestmentCurrencyValid)
                .flatMap(investment ->
                    tryFetchPriceInfo(investment)
                        .flatMap(priceInfo -> tryUpdateAndSaveInvestment(investment, priceInfo))
                )
                .orElseGet(() -> {
                    if (!investmentRepository.existsById(id)) {
                        logger.warn("Attempted to update current value for non-existent investment: {}", id);
                    }
                    // Specific logs for currency invalid, fetch failure, or save failure are in helper methods.
                    return null;
                });
    }

    private Optional<PriceInfo> tryFetchPriceInfo(Investment investment) {
        try {
            return Optional.ofNullable(fetchPriceInfoInternal(investment));
        } catch (IOException e) {
            logger.error("IOException fetching price info for investment {}: {}", investment.getId(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error fetching price info for investment {}: {}", investment.getId(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<Investment> tryUpdateAndSaveInvestment(Investment investment, PriceInfo priceInfo) {
        if (priceInfo.value() != null) {
            investment.setCurrentValue(priceInfo.value());
            investment.setLastUpdateDate(LocalDateTime.now());
            try {
                logger.debug("Updating investment {} with current value {}.", investment.getId(), priceInfo.value());
                return Optional.of(investmentRepository.save(investment));
            } catch (Exception e) {
                logger.error("Error saving investment {} after updating current value: {}", investment.getId(), e.getMessage(), e);
                return Optional.empty();
            }
        } else {
            logger.warn("PriceInfo value is null for investment {} ({}). Update aborted.", 
                        investment.getId(), investment.getTicker());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Investment sellInvestment(UUID id, SellInvestmentRequest request) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + id));

        if (!StatusInvestment.ACTIVE.equals(investment.getStatus())) { // Simplified check
            logger.warn("Attempted to sell investment {} which is not ACTIVE (Status: '{}').", id, investment.getStatus());
            throw new IllegalStateException("Cannot sell an investment that is not ACTIVE. Current status: " + investment.getStatus());
        }

        investment.setStatus(StatusInvestment.SOLD);
        investment.setSellPrice(request.sellPrice());
        investment.setLastUpdateDate(LocalDateTime.now());
        Investment soldInvestment = investmentRepository.save(investment);
        logger.info("Marked investment {} as SOLD at price {}.", id, request.sellPrice());
        return soldInvestment;
    }

    @Override
    @Transactional
    public Investment manuallyUpdateInvestmentCurrentValue(UUID investmentId, BigDecimal newCurrentValue) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + investmentId));

        if (!"Other".equalsIgnoreCase(investment.getType())) {
            logger.warn("Attempted to manually update current value for non-'Other' type investment ID: {}. Type: {}", investmentId, investment.getType());
            throw new IllegalArgumentException("Manual value update is only allowed for investments of type 'Other'.");
        }

        if (newCurrentValue == null) {
            throw new IllegalArgumentException("New current value cannot be null.");
        }
        if (newCurrentValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("New current value cannot be negative.");
        }

        logger.info("Manually updating current value for 'Other' investment ID: {} from {} to {}",
                investmentId, investment.getCurrentValue(), newCurrentValue);

        investment.setCurrentValue(newCurrentValue);
        investment.setLastUpdateDate(LocalDateTime.now());
        Investment savedInvestment = investmentRepository.save(investment);

        // Trigger portfolio value recalculation
        if (savedInvestment.getPortfolio() != null && savedInvestment.getPortfolio().getId() != null) {
            logger.info("Triggering portfolio value update for portfolio ID: {} after manual investment update.", savedInvestment.getPortfolio().getId());
            portfolioService.updatePortfolioValues(savedInvestment.getPortfolio().getId());
        } else {
            logger.warn("Cannot trigger portfolio value update for investment ID: {} as portfolio or portfolio ID is null.", investmentId);
        }

        return savedInvestment;
    }
}