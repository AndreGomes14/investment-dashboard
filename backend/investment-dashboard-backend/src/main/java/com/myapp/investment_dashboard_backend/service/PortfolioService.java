package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.dto.portfolio.UpdatePortfolioRequest;
import com.myapp.investment_dashboard_backend.exception.ResourceNotFoundException;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.model.UserSetting;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final CurrencyConversionService currencyConversionService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);

    @Autowired
    public PortfolioService(PortfolioRepository portfolioRepository, UserRepository userRepository, MarketDataService marketDataService, CurrencyConversionService currencyConversionService) {
        this.portfolioRepository = portfolioRepository;
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
}

