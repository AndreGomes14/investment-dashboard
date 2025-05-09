package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.dto.portfolio.InvestmentPerformanceDTO;
import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioSummaryResponse;
import com.myapp.investment_dashboard_backend.dto.portfolio.UpdatePortfolioRequest;
import com.myapp.investment_dashboard_backend.exception.ResourceNotFoundException;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.model.UserSetting;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import com.myapp.investment_dashboard_backend.utils.StatusInvestment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final CurrencyConversionService currencyConversionService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);

    @Autowired
    public PortfolioService(PortfolioRepository portfolioRepository,
                            InvestmentRepository investmentRepository,
                            UserRepository userRepository,
                            MarketDataService marketDataService,
                            CurrencyConversionService currencyConversionService) {
        this.portfolioRepository = portfolioRepository;
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
        this.marketDataService = marketDataService;
        this.currencyConversionService = currencyConversionService;
    }

    /**
     * Retrieves a portfolio by its ID, including its investments.
     *
     * @param id The ID of the portfolio to retrieve.
     * @return An Optional containing the portfolio if found, or an empty Optional if not found.
     */
    public Optional<Portfolio> getPortfolioByIdWithInvestments(UUID id) {
        return portfolioRepository.findByIdWithInvestments(id);
    }

    @Transactional
    public Portfolio createPortfolio(String portfolioName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));

        Portfolio portfolio = new Portfolio();
        portfolio.setName(portfolioName);
        portfolio.setUser(currentUser);
        portfolio.setTotalValue(BigDecimal.ZERO);

        return portfolioRepository.save(portfolio);
    }

    public List<Portfolio> getPortfoliosByCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));

        return portfolioRepository.findByUserId(currentUser.getId());
    }

    /**
     * Updates an existing portfolio's name and description.
     *
     * @param portfolioId The ID of the portfolio to update.
     * @param request The DTO containing the updated data.
     * @return The updated Portfolio object.
     * @throws ResourceNotFoundException if the portfolio is not found.
     * @throws AccessDeniedException if the current user doesn't own the portfolio.
     */
    @Transactional
    public Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        if (!portfolio.getUser().getUsername().equals(currentUsername)) {
            logger.warn("User '{}' attempted to update portfolio '{}' owned by '{}'",
                    currentUsername, portfolioId, portfolio.getUser().getUsername());
            throw new AccessDeniedException("User does not have permission to update this portfolio.");
        }

        logger.info("User '{}' updating portfolio '{}' (ID: {}). New name: '{}'",
                currentUsername, portfolio.getName(), portfolioId, request.getName());

        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());

        return portfolioRepository.save(portfolio);
    }

    /**
     * Calculates the total value of a portfolio converted to the user's preferred currency.
     *
     * @param portfolio The portfolio object.
     * @param user The user whose preferred currency will be used.
     * @return The total value as BigDecimal in the user's preferred currency, or null if calculation fails.
     */
    public BigDecimal calculatePortfolioValueInPreferredCurrency(Portfolio portfolio, User user) {
        if (user == null) {
            logger.error("Cannot calculate value in preferred currency: User object is null.");
            return null;
        }

        String preferredCurrency = user.getSettings().stream() // <- If user.getSettings() is null, this throws NPE
                .filter(s -> "preferredCurrency".equals(s.getKey()))
                .map(UserSetting::getValue)
                .findFirst()
                .orElse("USD");
        logger.debug("Calculating total value for portfolio {} in user {}'s preferred currency: {}",
                portfolio != null ? portfolio.getId() : "null", user.getUsername(), preferredCurrency);

        if (portfolio == null || portfolio.getInvestments() == null) {
            logger.warn("calculatePortfolioValueInPreferredCurrency called with null portfolio or investments.");
            return BigDecimal.ZERO; // Return zero if no investments
        }

        BigDecimal totalValueInPreferredCurrency = BigDecimal.ZERO;

        for (Investment investment : portfolio.getInvestments()) {
            if (investment.getTicker() == null || investment.getType() == null ||
                    investment.getCurrency() == null || investment.getCurrency().trim().isEmpty() ||
                    investment.getAmount() == null) {
                logger.warn("Skipping investment ID {} for total value calculation due to missing info.", investment.getId());
                continue;
            }

            try {
                PriceInfo priceInfo = marketDataService.getCurrentValue(
                        investment.getTicker(),
                        investment.getType(),
                        investment.getCurrency()
                );

                if (priceInfo != null && priceInfo.value() != null && priceInfo.currency() != null) {
                    if (!priceInfo.currency().equalsIgnoreCase(investment.getCurrency())) {
                        logger.error("Currency mismatch for investment {}: expected {}, but MarketDataService returned {}. Skipping for total value.",
                                investment.getId(), investment.getCurrency(), priceInfo.currency());
                        continue;
                    }

                    BigDecimal holdingValue = priceInfo.value().multiply(investment.getAmount());
                    String sourceCurrency = priceInfo.currency().toUpperCase();

                    // Convert to preferred currency if necessary
                    BigDecimal valueInPreferredCurrency;
                    if (sourceCurrency.equalsIgnoreCase(preferredCurrency)) {
                        valueInPreferredCurrency = holdingValue;
                    } else {
                        logger.debug("Converting investment {} ({}) value from {} to {}",
                                investment.getId(), investment.getTicker(), sourceCurrency, preferredCurrency);
                        valueInPreferredCurrency = currencyConversionService.convert(holdingValue, sourceCurrency, preferredCurrency);
                    }

                    if (valueInPreferredCurrency != null) {
                        totalValueInPreferredCurrency = totalValueInPreferredCurrency.add(valueInPreferredCurrency);
                    } else {
                        logger.error("Failed to convert value for investment {} ({}) from {} to {}. Skipping for total value.",
                                investment.getId(), investment.getTicker(), sourceCurrency, preferredCurrency);
                        return null;
                    }

                } else {
                    logger.warn("Could not retrieve valid PriceInfo for investment {} ({}-{}). Skipping for total value.",
                            investment.getId(), investment.getType(), investment.getTicker());
                    return null;
                }
            } catch (IOException e) {
                logger.error("IOException calculating value for investment {} ({}-{}) in portfolio {}: {}. Cannot calculate total value.",
                        investment.getId(), investment.getType(), investment.getTicker(), portfolio.getId(), e.getMessage());
                return null;
            } catch (Exception e) {
                logger.error("Unexpected error calculating value for investment {} ({}-{}) in portfolio {}: {}. Cannot calculate total value.",
                        investment.getId(), investment.getType(), investment.getTicker(), portfolio.getId(), e.getMessage());
                return null;
            }
        }

        return totalValueInPreferredCurrency.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Updates the values of all investments and recalculates the total portfolio value
     * in the owner's preferred currency.
     *
     * @param portfolioId The ID of the portfolio to update.
     * @return The updated portfolio with the new total value, or null if the update fails.
     */
    @Transactional
    public Portfolio updatePortfolioValues(UUID portfolioId) {
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByIdWithInvestments(portfolioId);
        if (portfolioOptional.isPresent()) {
            Portfolio portfolio = portfolioOptional.get();
            User owner = portfolio.getUser(); // Get the owner to find preferred currency
            if (owner == null) {
                logger.error("Portfolio {} has no associated user. Cannot update total value correctly.", portfolioId);
                return null;
            }

            boolean individualUpdateFailed = false;
            for (Investment investment : portfolio.getInvestments()) {
                if (investment.getTicker() == null || investment.getType() == null || investment.getCurrency() == null) {
                    logger.warn("Skipping update for investment ID {} due to missing ticker, type, or currency.", investment.getId());
                    continue;
                }
                try {
                    PriceInfo priceInfo = marketDataService.getCurrentValue(
                            investment.getTicker(), investment.getType(), investment.getCurrency());
                    if (priceInfo != null && priceInfo.value() != null) {
                        investment.setCurrentValue(priceInfo.value());
                        investment.setLastUpdateDate(LocalDateTime.now());
                    } else {
                        logger.warn("Failed to update current value for investment {} ({}-{}) in portfolio {}. Current value will remain unchanged.",
                                investment.getId(), investment.getType(), investment.getTicker(), portfolioId);
                        individualUpdateFailed = true;
                    }
                } catch (IOException e) {
                    logger.error("IOException updating individual investment value for {} ({}-{}): {}. Skipping update.",
                            investment.getId(), investment.getType(), investment.getTicker(), e.getMessage());
                    individualUpdateFailed = true;
                } catch (Exception e) {
                    logger.error("Unexpected error updating individual investment value for {} ({}-{}): {}. Skipping update.",
                            investment.getId(), investment.getType(), investment.getTicker(), e.getMessage());
                    individualUpdateFailed = true;
                }
            }

            BigDecimal newTotalValue = calculatePortfolioValueInPreferredCurrency(portfolio, owner);

            if (newTotalValue != null) {
                portfolio.setTotalValue(newTotalValue); // Update the single totalValue field
                Portfolio savedPortfolio = portfolioRepository.save(portfolio);

                Set<UserSetting> ownerSettings = owner.getSettings();
                String loggedPreferredCurrency = (ownerSettings != null)
                        ? ownerSettings.stream()
                        .filter(s -> s != null && "preferredCurrency".equals(s.getKey()))
                        .map(UserSetting::getValue)
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse("USD")
                        : "USD";

                logger.info("Successfully updated portfolio {} total value to {} {}",
                        portfolioId,
                        newTotalValue,
                        loggedPreferredCurrency);

                if (individualUpdateFailed) {
                    logger.warn("Portfolio {} total value updated, but one or more individual investment value updates failed.", portfolioId);
                }
                return savedPortfolio;
            } else {
                logger.error("Failed to calculate new total value in preferred currency for portfolio {}. Total value not updated.", portfolioId);
                return null;
            }
        } else {
            logger.warn("Attempted to update values for non-existent portfolio: {}", portfolioId);
            return null;
        }
    }

    /**
     * Deletes a portfolio by its ID after verifying ownership.
     *
     * @param portfolioId The ID of the portfolio to delete.
     * @throws ResourceNotFoundException if the portfolio does not exist.
     * @throws AccessDeniedException if the current user does not own the portfolio.
     */
    @Transactional
    public void deletePortfolio(UUID portfolioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        if (!portfolio.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("User does not have permission to delete this portfolio");
        }

        portfolioRepository.delete(portfolio);
        logger.info("Portfolio with id {} deleted successfully by user {}", portfolioId, currentUsername);
    }

    /**
     * Gets the summary data for all portfolios owned by the current user.
     *
     * @return PortfolioSummaryResponse containing aggregated metrics and investments.
     */
    public PortfolioSummaryResponse getOverallSummary() {
        User currentUser = getCurrentUser();
        List<Portfolio> userPortfolios = portfolioRepository.findByUserId(currentUser.getId());
        if (userPortfolios.isEmpty()) {
            return createEmptySummaryResponse("All Portfolios (None Found)");
        }

        // Fetch all investments for all user portfolios in one go
        List<UUID> portfolioIds = userPortfolios.stream().map(Portfolio::getId).collect(Collectors.toList());
        List<Investment> allInvestments = investmentRepository.findByPortfolio_IdIn(portfolioIds);

        return calculateSummary(allInvestments, "All Portfolios");
    }

    /**
     * Gets the summary data for a specific portfolio owned by the current user.
     *
     * @param portfolioId The ID of the portfolio.
     * @return PortfolioSummaryResponse containing metrics and investments for the specified portfolio.
     * @throws ResourceNotFoundException if the portfolio is not found.
     * @throws AccessDeniedException if the user does not own the portfolio.
     */
    public PortfolioSummaryResponse getPortfolioSummary(UUID portfolioId) {
        User currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        // Verify ownership
        if (!portfolio.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("User does not have permission to view this portfolio summary");
        }

        // Fetch investments specifically for this portfolio
        List<Investment> investments = investmentRepository.findByPortfolioId(portfolioId);

        return calculateSummary(investments, portfolio.getName());
    }

    /**
     * Helper method to calculate summary metrics from a list of investments.
     *
     * @param investments List of investments (can be from one or multiple portfolios).
     * @param portfolioName Name to display in the summary (e.g., "Specific Portfolio" or "All Portfolios").
     * @return Calculated PortfolioSummaryResponse.
     */
    private PortfolioSummaryResponse calculateSummary(List<Investment> investments, String portfolioName) {
        List<Investment> activeInvestments = investments.stream()
                .filter(inv -> inv.getStatus() != null && inv.getStatus().trim().equals(StatusInvestment.ACTIVE))
                .toList();

        List<Investment> soldInvestments = investments.stream()
                .filter(inv -> inv.getStatus() != null && inv.getStatus().trim().equals(StatusInvestment.SOLD))
                .toList();

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        Map<String, BigDecimal> valueByType = new HashMap<>();

        // Track best/worst performers
        InvestmentPerformanceDTO bestPerformer = null;
        InvestmentPerformanceDTO worstPerformer = null;
        BigDecimal maxPnlPercent = new BigDecimal(Integer.MIN_VALUE); // Initialize low
        BigDecimal minPnlPercent = new BigDecimal(Integer.MAX_VALUE); // Initialize high

        // Calculate metrics for ACTIVE investments
        for (Investment inv : activeInvestments) {
            BigDecimal purchasePrice = inv.getPurchasePrice() != null ? inv.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal amount = inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO;
            BigDecimal currentValue = inv.getCurrentValue(); // Assumes this is already fetched/updated

            // Fetch current value if missing (same logic as before)
            if (currentValue == null && inv.getTicker() != null && inv.getType() != null && inv.getCurrency() != null) {
                try {
                    PriceInfo priceInfo = marketDataService.getCurrentValue(inv.getTicker(), inv.getType(), inv.getCurrency());
                    if(priceInfo != null) currentValue = priceInfo.value();
                } catch (IOException e) {
                    logger.warn("Could not fetch current value for active investment {} during summary calculation: {}", inv.getId(), e.getMessage());
                }
            }
            currentValue = (currentValue != null) ? currentValue : purchasePrice; // Fallback

            BigDecimal holdingCost = purchasePrice.multiply(amount);
            BigDecimal holdingValue = currentValue.multiply(amount);

            totalCostBasis = totalCostBasis.add(holdingCost);
            totalValue = totalValue.add(holdingValue);

            // Calculate individual PnL % for best/worst calculation
            BigDecimal individualPnlAbsolute = holdingValue.subtract(holdingCost);
            BigDecimal individualPnlPercent = BigDecimal.ZERO;
            if (holdingCost.compareTo(BigDecimal.ZERO) > 0) {
                individualPnlPercent = individualPnlAbsolute
                        .divide(holdingCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else if (holdingValue.compareTo(BigDecimal.ZERO) > 0) {
                individualPnlPercent = new BigDecimal(Integer.MAX_VALUE); // Represent as very large positive for comparison
            }

            // Update best performer
            if (individualPnlPercent.compareTo(maxPnlPercent) > 0) {
                maxPnlPercent = individualPnlPercent;
                bestPerformer = new InvestmentPerformanceDTO(
                        inv.getTicker(), inv.getType(),
                        individualPnlPercent.setScale(2, RoundingMode.HALF_UP),
                        currentValue.setScale(2, RoundingMode.HALF_UP),
                        holdingValue.setScale(2, RoundingMode.HALF_UP)
                );
            }

            // Update worst performer
            if (individualPnlPercent.compareTo(minPnlPercent) < 0) {
                minPnlPercent = individualPnlPercent;
                worstPerformer = new InvestmentPerformanceDTO(
                        inv.getTicker(), inv.getType(),
                        individualPnlPercent.setScale(2, RoundingMode.HALF_UP),
                        currentValue.setScale(2, RoundingMode.HALF_UP),
                        holdingValue.setScale(2, RoundingMode.HALF_UP)
                );
            }

            // For allocation - use holding value
            String type = inv.getType() != null ? inv.getType() : "Unknown";
            valueByType.put(type, valueByType.getOrDefault(type, BigDecimal.ZERO).add(holdingValue));
        }

        // Calculate Unrealized PnL
        BigDecimal unrealizedPnlAbsolute = totalValue.subtract(totalCostBasis);
        BigDecimal unrealizedPnlPercentage = BigDecimal.ZERO;
        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnlPercentage = unrealizedPnlAbsolute
                    .divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Calculate Realized PnL for SOLD investments
        BigDecimal realizedPnlAbsolute = BigDecimal.ZERO;
        for (Investment inv : soldInvestments) {
            if (inv.getSellPrice() != null && inv.getPurchasePrice() != null && inv.getAmount() != null) {
                BigDecimal gainLossPerUnit = inv.getSellPrice().subtract(inv.getPurchasePrice());
                realizedPnlAbsolute = realizedPnlAbsolute.add(gainLossPerUnit.multiply(inv.getAmount()));
            }
        }

        // Calculate Asset Allocation Percentage
        Map<String, BigDecimal> assetAllocationPercentage = new HashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : valueByType.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                assetAllocationPercentage.put(entry.getKey(), percentage.setScale(2, RoundingMode.HALF_UP)); // Store with 2 decimal places
            }
        }

        PortfolioSummaryResponse.SummaryMetrics metrics = new PortfolioSummaryResponse.SummaryMetrics(
                portfolioName,
                totalValue.setScale(2, RoundingMode.HALF_UP),
                totalCostBasis.setScale(2, RoundingMode.HALF_UP),
                unrealizedPnlAbsolute.setScale(2, RoundingMode.HALF_UP),
                unrealizedPnlPercentage.setScale(2, RoundingMode.HALF_UP),
                realizedPnlAbsolute.setScale(2, RoundingMode.HALF_UP),
                assetAllocationPercentage,
                activeInvestments.size(), // Add count
                bestPerformer, // Add best
                worstPerformer // Add worst
        );

        return new PortfolioSummaryResponse(metrics, activeInvestments, soldInvestments);
    }

    /**
     * Helper to get the currently authenticated user.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));
    }

    /**
     * Helper to create an empty summary response.
     */
    private PortfolioSummaryResponse createEmptySummaryResponse(String name) {
        PortfolioSummaryResponse.SummaryMetrics metrics = new PortfolioSummaryResponse.SummaryMetrics(
                name,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyMap(),
                0, null, null
        );
        return new PortfolioSummaryResponse(metrics, Collections.emptyList(), Collections.emptyList());
    }
}

