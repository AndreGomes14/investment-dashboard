package com.myapp.investment_dashboard_backend.service.impl;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentPerformanceDTO;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;
import com.myapp.investment_dashboard_backend.dto.portfolio.CategorizedInvestments;
import com.myapp.investment_dashboard_backend.dto.portfolio.HistoricalDataPointDTO;
import com.myapp.investment_dashboard_backend.dto.portfolio.InvestmentUpdateResult;
import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioSummaryAggregators;
import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioSummaryResponse;
import com.myapp.investment_dashboard_backend.dto.portfolio.PortfolioUpdateResults;
import com.myapp.investment_dashboard_backend.dto.portfolio.UpdatePortfolioRequest;
import com.myapp.investment_dashboard_backend.exception.ResourceNotFoundException;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.model.UserPortfolioValueHistory;
import com.myapp.investment_dashboard_backend.model.UserSetting;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.repository.UserPortfolioValueHistoryRepository;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import com.myapp.investment_dashboard_backend.service.CurrencyConversionService;
import com.myapp.investment_dashboard_backend.service.MarketDataService;
import com.myapp.investment_dashboard_backend.service.PortfolioService;
import com.myapp.investment_dashboard_backend.utils.StatusInvestment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final UserPortfolioValueHistoryRepository userPortfolioValueHistoryRepository;
    private final MarketDataService marketDataService;
    private final CurrencyConversionService currencyConversionService;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public PortfolioServiceImpl(PortfolioRepository portfolioRepository,
                            InvestmentRepository investmentRepository,
                            UserRepository userRepository,
                            UserPortfolioValueHistoryRepository userPortfolioValueHistoryRepository,
                            MarketDataService marketDataService,
                            CurrencyConversionService currencyConversionService) {
        this.portfolioRepository = portfolioRepository;
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
        this.userPortfolioValueHistoryRepository = userPortfolioValueHistoryRepository;
        this.marketDataService = marketDataService;
        this.currencyConversionService = currencyConversionService;
    }

    @Override
    public Optional<Portfolio> getPortfolioByIdWithInvestments(UUID id) {
        return portfolioRepository.findByIdWithInvestments(id);
    }

    @Override
    @Transactional
    public Portfolio createPortfolio(String portfolioName) {
        User currentUser = getCurrentUser();

        Portfolio portfolio = new Portfolio();
        portfolio.setName(portfolioName);
        portfolio.setUser(currentUser);
        portfolio.setTotalValue(BigDecimal.ZERO);

        return portfolioRepository.save(portfolio);
    }

    @Override
    public List<Portfolio> getPortfoliosByCurrentUser() {
        User currentUser = getCurrentUser();
        return portfolioRepository.findByUserId(currentUser.getId());
    }

    @Override
    @Transactional
    public Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioRequest request) {
        User currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        checkPortfolioOwnership(portfolio, currentUser, "update");

        logger.info("User '{}' updating portfolio '{}' (ID: {}). New name: '{}'",
                currentUser.getUsername(), portfolio.getName(), portfolioId, request.getName());

        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());

        return portfolioRepository.save(portfolio);
    }

    @Override
    public BigDecimal calculatePortfolioValueInPreferredCurrency(Portfolio portfolio, User user) {
        if (user == null) {
            logger.error("Cannot calculate value in preferred currency: User object is null.");
            return null;
        }

        String preferredCurrency = getUserPreferredCurrency(user);

        logger.debug("Calculating total value for portfolio {} in user '{}'s preferred currency: {}",
                portfolio != null ? portfolio.getId() : "null", user.getUsername(), preferredCurrency);

        if (portfolio == null || portfolio.getInvestments() == null) {
            logger.warn("calculatePortfolioValueInPreferredCurrency called with null portfolio or investments.");
            return BigDecimal.ZERO;
        }

        BigDecimal totalValueInPreferredCurrency = BigDecimal.ZERO;

        for (Investment investment : portfolio.getInvestments()) {
            BigDecimal valueInPreferredCurrency = processSingleInvestmentForValue(investment, preferredCurrency, portfolio.getId());
            if (valueInPreferredCurrency != null) {
                totalValueInPreferredCurrency = totalValueInPreferredCurrency.add(valueInPreferredCurrency);
            } else {
                logger.error("[CalcVal Portfolio {}] Failed to process investment {}. Total calculation for portfolio will return null.",
                             portfolio.getId(), investment.getId());
                return null;
            }
        }
        logger.info("[CalcVal Portfolio {} User {}] Final calculated totalValueInPreferredCurrency before scaling: {}",
            portfolio.getId(), user.getUsername(), totalValueInPreferredCurrency);
        return totalValueInPreferredCurrency.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal processSingleInvestmentForValue(Investment investment, String preferredCurrency, UUID portfolioId) {
        String logPrefix = String.format("[CalcVal Portfolio %s Inv %s]", portfolioId, investment.getId());
        logger.debug("{} Processing investment. Ticker: {}, Type: {}, Currency: {}, Amount: {}",
            logPrefix, investment.getTicker(), investment.getType(), investment.getCurrency(), investment.getAmount());

        if (isInvestmentDataInvalid(investment, logPrefix)) {
            return BigDecimal.ZERO; 
        }

        try {
            // Special handling for custom assets (type 'Other') which rely solely on user-provided currentValue.
            if ("Other".equalsIgnoreCase(investment.getType())) {
                if (investment.getCurrentValue() == null) {
                    logger.warn("{} Investment type 'Other' does not have currentValue set. Skipping and returning null.", logPrefix);
                    return null;
                }

                BigDecimal holdingValue = investment.getCurrentValue().multiply(investment.getAmount());

                String sourceCurrency = investment.getCurrency().toUpperCase();
                if (sourceCurrency.equalsIgnoreCase(preferredCurrency)) {
                    logger.debug("{} Using user-provided currentValue for 'Other' asset. HoldingValue: {} {} (no conversion needed)", logPrefix, holdingValue, sourceCurrency);
                    return holdingValue;
                } else {
                    logger.debug("{} Converting user-provided holdingValue {} from {} to preferred currency {}", logPrefix, holdingValue, sourceCurrency, preferredCurrency);
                    BigDecimal converted = currencyConversionService.convert(holdingValue, sourceCurrency, preferredCurrency);
                    if (converted == null) {
                        logger.error("{} Currency conversion failed for 'Other' asset from {} to {}. Returning null.", logPrefix, sourceCurrency, preferredCurrency);
                        return null;
                    }
                    return converted;
                }
            }

            Optional<PriceInfo> priceInfoOpt = fetchAndValidateCorePriceInfo(investment, logPrefix);
            if (priceInfoOpt.isEmpty()) {
                return null; // Error already logged by helper
            }
            PriceInfo priceInfo = priceInfoOpt.get();

            if (!ensureCurrencyConsistency(priceInfo, investment, logPrefix)) {
                return null; // Error already logged by helper
            }

            return calculateFinalValue(priceInfo, investment, preferredCurrency, logPrefix);

        } catch (IOException e) {
            logger.error("{} IOException during investment value processing: {}. Investment calculation fails.",
                    logPrefix, e.getMessage(), e);
            return null; 
        } catch (Exception e) {
            logger.error("{} Unexpected error during investment value processing: {}. Investment calculation fails.",
                    logPrefix, e.getMessage(), e);
            return null; 
        }
    }

    private boolean isInvestmentDataInvalid(Investment investment, String logPrefix) {
        if (investment.getTicker() == null || investment.getTicker().trim().isEmpty() ||
            investment.getType() == null || investment.getType().trim().isEmpty() ||
            investment.getCurrency() == null || investment.getCurrency().trim().isEmpty() ||
            investment.getAmount() == null) {
            logger.warn("{} Skipping investment for total value calculation due to missing or invalid essential info (Ticker, Type, Currency, Amount).", logPrefix);
            return true;
        }
        return false;
    }

    private Optional<PriceInfo> fetchAndValidateCorePriceInfo(Investment investment, String logPrefix) throws IOException {
        PriceInfo priceInfo = marketDataService.getCurrentValue(
                investment.getTicker(),
                investment.getType(),
                investment.getCurrency()
        );
        logger.debug("{} marketDataService.getCurrentValue returned: PriceInfo[value={}, currency={}]",
            logPrefix, priceInfo != null ? priceInfo.value() : "null", priceInfo != null ? priceInfo.currency() : "null");

        if (priceInfo == null || priceInfo.value() == null || priceInfo.currency() == null) {
            logger.warn("{} Could not retrieve valid PriceInfo (priceInfo, its value, or its currency is null). PriceInfo: {}. Investment calculation fails.",
                    logPrefix, priceInfo);
            return Optional.empty();
        }
        return Optional.of(priceInfo);
    }

    private boolean ensureCurrencyConsistency(PriceInfo priceInfo, Investment investment, String logPrefix) {
        if (!priceInfo.currency().equalsIgnoreCase(investment.getCurrency())) {
            logger.error("{} Currency mismatch: investment currency '{}', priceInfo currency '{}'. Investment calculation fails.",
                    logPrefix, investment.getCurrency(), priceInfo.currency());
            return false;
        }
        return true;
    }

    private BigDecimal calculateFinalValue(PriceInfo priceInfo, Investment investment, String preferredCurrency, String logPrefix) {
        BigDecimal holdingValue = priceInfo.value().multiply(investment.getAmount());
        String sourceCurrency = priceInfo.currency().toUpperCase();
        logger.debug("{} Calculated holdingValue: {} (price {} * amount {}), sourceCurrency: {}",
            logPrefix, holdingValue, priceInfo.value(), investment.getAmount(), sourceCurrency);

        if (sourceCurrency.equalsIgnoreCase(preferredCurrency)) {
            logger.debug("{} Source currency {} matches preferred currency {}. valueInPreferredCurrency: {}",
                logPrefix, sourceCurrency, preferredCurrency, holdingValue);
            return holdingValue;
        } else {
            logger.debug("{} Converting holdingValue {} from {} to preferredCurrency {}.",
                logPrefix, holdingValue, sourceCurrency, preferredCurrency);
            BigDecimal convertedValue = currencyConversionService.convert(holdingValue, sourceCurrency, preferredCurrency);
            if (convertedValue == null) {
                logger.error("{} Failed to convert holding value from {} to {}. Investment calculation fails.", 
                    logPrefix, sourceCurrency, preferredCurrency);
                return null;
            }
            logger.debug("{} currencyConversionService.convert returned: {}", logPrefix, convertedValue);
            return convertedValue;
        }
    }

    @Override
    @Transactional
    public Portfolio updatePortfolioValues(UUID portfolioId) {
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByIdWithInvestments(portfolioId);
        if (portfolioOptional.isEmpty()) {
            logger.warn("Attempted to update values for non-existent portfolio: {}", portfolioId);
            return null;
        }

        Portfolio portfolio = portfolioOptional.get();
        User owner = portfolio.getUser();
        if (owner == null) {
            logger.error("Portfolio {} has no associated user. Cannot update total value correctly.", portfolioId);
            return null;
        }

        boolean anyIndividualUpdateFailed = updateMarketValuesForPortfolioInvestments(portfolio);

        BigDecimal newTotalValueInPreferredCurrency = calculatePortfolioValueInPreferredCurrency(portfolio, owner);

        if (newTotalValueInPreferredCurrency != null) {
            portfolio.setTotalValue(newTotalValueInPreferredCurrency);
            Portfolio savedPortfolio = portfolioRepository.save(portfolio);
            recordUserTotalPortfolioValue(owner);

            String loggedPreferredCurrency = getUserPreferredCurrency(owner);
            logger.info("Successfully updated portfolio {} total value to {} {}",
                    portfolioId, newTotalValueInPreferredCurrency, loggedPreferredCurrency);

            if (anyIndividualUpdateFailed) {
                logger.warn("Portfolio {} total value updated, but one or more individual investment value updates failed.", portfolioId);
            }
            return savedPortfolio;
        } else {
            logger.error("Failed to calculate new total value in preferred currency for portfolio {}. Total value not updated.", portfolioId);
            return portfolioRepository.findById(portfolioId).orElse(null);
        }
    }

    private boolean updateMarketValuesForPortfolioInvestments(Portfolio portfolio) {
        boolean anyUpdateFailed = false;
        for (Investment investment : portfolio.getInvestments()) {
            boolean success = updateSingleInvestmentMarketValue(investment, portfolio.getId());
            if (!success) {
                anyUpdateFailed = true;
            }
        }
        return anyUpdateFailed;
    }

    private boolean updateSingleInvestmentMarketValue(Investment investment, UUID portfolioId) {
        if ("Other".equalsIgnoreCase(investment.getType())) {
            logger.info("Skipping automatic value update for investment ID {} in portfolio {} because its type is 'Other'.", investment.getId(), portfolioId);
            return true; // Indicate that no update was attempted, not an error for this type.
        }
        if (investment.getTicker() == null || investment.getType() == null || investment.getCurrency() == null) {
            logger.warn("Skipping update for investment ID {} in portfolio {} due to missing ticker, type, or currency.", investment.getId(), portfolioId);
            return true;
        }
        try {
            PriceInfo priceInfo = marketDataService.getCurrentValue(
                    investment.getTicker(), investment.getType(), investment.getCurrency());
            if (priceInfo != null && priceInfo.value() != null) {
                investment.setCurrentValue(priceInfo.value());
                investment.setLastUpdateDate(LocalDateTime.now());
                return true;
            } else {
                logger.warn("Failed to update current value for investment {} ({}-{}) in portfolio {}. Current value will remain unchanged.",
                        investment.getId(), investment.getType(), investment.getTicker(), portfolioId);
                return false;
            }
        } catch (IOException e) {
            logger.error("IOException updating individual investment value for {} ({}-{}) in portfolio {}: {}. Skipping update.",
                    investment.getId(), investment.getType(), investment.getTicker(), portfolioId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error updating individual investment value for {} ({}-{}) in portfolio {}: {}. Skipping update.",
                    investment.getId(), investment.getType(), investment.getTicker(), portfolioId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void deletePortfolio(UUID portfolioId) {
        User currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        checkPortfolioOwnership(portfolio, currentUser, "delete");

        portfolioRepository.delete(portfolio);
        logger.info("Portfolio with id {} deleted successfully by user {}", portfolioId, currentUser.getUsername());
    }

    @Override
    public PortfolioSummaryResponse getOverallSummary() {
        User currentUser = getCurrentUser();
        List<Portfolio> userPortfolios = portfolioRepository.findByUserId(currentUser.getId());
        if (userPortfolios.isEmpty()) {
            return createEmptySummaryResponse();
        }

        List<UUID> portfolioIds = userPortfolios.stream()
                .map(Portfolio::getId)
                .toList();
        List<Investment> allInvestments = investmentRepository.findByPortfolio_IdIn(portfolioIds);

        return calculateSummary(allInvestments, "All Portfolios");
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummary(UUID portfolioId) {
        User currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        checkPortfolioOwnership(portfolio, currentUser, "view summary of");

        List<Investment> investments = investmentRepository.findByPortfolioId(portfolioId);

        return calculateSummary(investments, portfolio.getName());
    }

    private CategorizedInvestments categorizeInvestments(List<Investment> allInvestments) {
        List<Investment> active = allInvestments.stream()
                .filter(inv -> inv.getStatus() != null && StatusInvestment.ACTIVE.equals(inv.getStatus().trim()))
                .toList();
        List<Investment> sold = allInvestments.stream()
                .filter(inv -> inv.getStatus() != null && StatusInvestment.SOLD.equals(inv.getStatus().trim()))
                .toList();
        return new CategorizedInvestments(active, sold);
    }

    private PortfolioSummaryResponse calculateSummary(List<Investment> investments, String portfolioName) {
        CategorizedInvestments categorized = categorizeInvestments(investments);
        List<Investment> activeInvestments = categorized.active();
        List<Investment> soldInvestments = categorized.sold();

        Map<String, BigDecimal> valueByType = new HashMap<>();
        Map<String, BigDecimal> valueByCurrency = new HashMap<>();

        // Determine preferred currency of current user (default to USD if not set)
        User currentUser = getCurrentUser();
        String preferredCurrency = getUserPreferredCurrency(currentUser);

        logger.debug("Calculating summary for portfolio '{}' using preferred currency '{}'.", portfolioName, preferredCurrency);

        PortfolioSummaryAggregators aggregators = new PortfolioSummaryAggregators();

        for (Investment inv : activeInvestments) {
            processActiveInvestmentForSummary(inv, aggregators, valueByType, valueByCurrency, preferredCurrency);
        }
        
        PortfolioSummaryResponse.SummaryMetrics metrics = buildSummaryMetrics(
            portfolioName, 
            aggregators, 
            valueByType, 
            valueByCurrency,
            soldInvestments, 
            activeInvestments.size(),
            preferredCurrency
        );
        
        return new PortfolioSummaryResponse(metrics, activeInvestments, soldInvestments);
    }

    private PortfolioSummaryResponse.SummaryMetrics buildSummaryMetrics(
            String portfolioName,
            PortfolioSummaryAggregators aggregators,
            Map<String, BigDecimal> valueByType,
            Map<String, BigDecimal> valueByCurrency,
            List<Investment> soldInvestments,
            int activeInvestmentCount,
            String preferredCurrency) {

        BigDecimal totalValue = aggregators.totalValue;
        BigDecimal totalCostBasis = aggregators.totalCostBasis;

        BigDecimal unrealizedPnlAbsolute = totalValue.subtract(totalCostBasis);
        BigDecimal unrealizedPnlPercentage = calculatePnlPercentage(unrealizedPnlAbsolute, totalCostBasis);
        BigDecimal realizedPnlAbsolute = calculateRealizedPnlInPreferredCurrency(soldInvestments, preferredCurrency);
        Map<String, BigDecimal> assetAllocationPercentage = calculateAssetAllocation(valueByType, totalValue);
        Map<String, BigDecimal> currencyAllocationPercentage = calculateAssetAllocation(valueByCurrency, totalValue);

        return new PortfolioSummaryResponse.SummaryMetrics(
                portfolioName,
                totalValue.setScale(2, RoundingMode.HALF_UP),
                totalCostBasis.setScale(2, RoundingMode.HALF_UP),
                unrealizedPnlAbsolute.setScale(2, RoundingMode.HALF_UP),
                unrealizedPnlPercentage.setScale(2, RoundingMode.HALF_UP),
                realizedPnlAbsolute.setScale(2, RoundingMode.HALF_UP),
                assetAllocationPercentage,
                currencyAllocationPercentage,
                activeInvestmentCount,
                aggregators.bestPerformer,
                aggregators.worstPerformer
        );
    }

    private void processActiveInvestmentForSummary(Investment inv, PortfolioSummaryAggregators aggregators, Map<String, BigDecimal> valueByType, Map<String, BigDecimal> valueByCurrency, String preferredCurrency) {
        BigDecimal purchasePrice = inv.getPurchasePrice() != null ? inv.getPurchasePrice() : BigDecimal.ZERO;
        BigDecimal amount = inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO;
        
        BigDecimal resolvedCurrentValue = resolveCurrentValueForSummary(inv, purchasePrice);

        calculateAndAggregateInvestmentSummaryMetrics(inv, resolvedCurrentValue, purchasePrice, amount, aggregators, valueByType, valueByCurrency, preferredCurrency);
    }

    private BigDecimal resolveCurrentValueForSummary(Investment inv, BigDecimal purchasePriceIfFallbackNeeded) {
        if ("Other".equalsIgnoreCase(inv.getType())) {
            logger.debug("Investment {} is type 'Other'. Using its stored current value for summary.", inv.getId());
            return (inv.getCurrentValue() != null) ? inv.getCurrentValue() : purchasePriceIfFallbackNeeded;
        }
        BigDecimal currentValue = inv.getCurrentValue(); // Start with existing current value

        // Fetch current value from market data service if it's not already set on the investment object
        if (currentValue == null && inv.getTicker() != null && inv.getType() != null && inv.getCurrency() != null) {
            try {
                PriceInfo priceInfo = marketDataService.getCurrentValue(inv.getTicker(), inv.getType(), inv.getCurrency());
                if (priceInfo != null && priceInfo.value() != null) {
                    currentValue = priceInfo.value();
                } else {
                    logger.warn("PriceInfo or its value was null for investment {} (Ticker: {}) during summary. Fallback may apply.", inv.getId(), inv.getTicker());
                }
            } catch (IOException e) {
                logger.warn("Could not fetch current value for active investment {} (Ticker: {}) during summary calculation: {}. Fallback may apply.", 
                             inv.getId(), inv.getTicker(), e.getMessage());
            }
        }
        // Fallback to purchase price if current value is still null (e.g., fetch failed, original was null, or PriceInfo was invalid)
        return (currentValue != null) ? currentValue : purchasePriceIfFallbackNeeded;
    }

    private void calculateAndAggregateInvestmentSummaryMetrics(
            Investment inv,
            BigDecimal currentValue,
            BigDecimal purchasePrice,
            BigDecimal amount,
            PortfolioSummaryAggregators aggregators,
            Map<String, BigDecimal> valueByType,
            Map<String, BigDecimal> valueByCurrency,
            String preferredCurrency) {

        BigDecimal holdingCost = purchasePrice.multiply(amount);
        BigDecimal holdingValue = currentValue.multiply(amount);

        // Convert amounts to preferred currency if required
        String sourceCurrency = inv.getCurrency() != null ? inv.getCurrency().toUpperCase() : "UNK";
        if (!sourceCurrency.equalsIgnoreCase(preferredCurrency)) {
            BigDecimal convertedCost = currencyConversionService.convert(holdingCost, sourceCurrency, preferredCurrency);
            BigDecimal convertedValue = currencyConversionService.convert(holdingValue, sourceCurrency, preferredCurrency);

            if (convertedCost != null) {
                holdingCost = convertedCost;
            } else {
                logger.error("Failed to convert holding cost for investment {} from {} to {} during summary.", inv.getId(), sourceCurrency, preferredCurrency);
            }

            if (convertedValue != null) {
                holdingValue = convertedValue;
            } else {
                logger.error("Failed to convert holding value for investment {} from {} to {} during summary.", inv.getId(), sourceCurrency, preferredCurrency);
            }
        }

        // Set totalCost on the investment object
        inv.setTotalCost(holdingCost.setScale(2, RoundingMode.HALF_UP));

        aggregators.totalCostBasis = aggregators.totalCostBasis.add(holdingCost);
        aggregators.totalValue = aggregators.totalValue.add(holdingValue);

        BigDecimal individualPnlAbsolute = holdingValue.subtract(holdingCost);
        BigDecimal individualPnlPercent = calculatePnlPercentage(individualPnlAbsolute, holdingCost);

        inv.setProfitOrLoss(individualPnlAbsolute.setScale(2, RoundingMode.HALF_UP));

        // For percentProfit, store the raw decimal ratio (e.g., 0.3129 for 31.29%) as Angular's percent pipe will multiply by 100
        if (holdingCost.compareTo(BigDecimal.ZERO) != 0) {
            // Direct calculation of ratio: PnL / Cost
            inv.setPercentProfit(individualPnlAbsolute.divide(holdingCost, 4, RoundingMode.HALF_UP));
        } else if (individualPnlAbsolute.compareTo(BigDecimal.ZERO) > 0) {
            // Cost is 0, PnL is positive. Percentage is effectively infinite.
            // The calculatePnlPercentage method returns Integer.MAX_VALUE in this case.
            // To provide a ratio, we represent this as a very large number (MAX_VALUE / 100 for consistency if needed, or just a high fixed number).
            // Let's use the value from individualPnlPercent (which would be Integer.MAX_VALUE) and divide it by 100 for the DTO.
            inv.setPercentProfit(BigDecimal.valueOf(Integer.MAX_VALUE).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        } else {
            // Cost is 0, and PnL is zero or negative. Ratio is 0.
            inv.setPercentProfit(BigDecimal.ZERO);
        }

        updatePerformanceTrackers(inv, individualPnlPercent, currentValue, holdingValue, aggregators);

        // Accumulate by currency as well
        valueByCurrency.put(sourceCurrency, valueByCurrency.getOrDefault(sourceCurrency, BigDecimal.ZERO).add(holdingValue));

        String type = inv.getType() != null ? inv.getType() : "Unknown";
        valueByType.put(type, valueByType.getOrDefault(type, BigDecimal.ZERO).add(holdingValue));
    }

    private void updatePerformanceTrackers(Investment inv, BigDecimal individualPnlPercent, BigDecimal currentPrice, BigDecimal holdingValue, PortfolioSummaryAggregators aggregators) {
        BigDecimal pnlPercentForDTO = individualPnlPercent.setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentPriceForDTO = currentPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal holdingValueForDTO = holdingValue.setScale(2, RoundingMode.HALF_UP);

        if (individualPnlPercent.compareTo(aggregators.maxPnlPercent) > 0) {
            aggregators.maxPnlPercent = individualPnlPercent;
            aggregators.bestPerformer = new InvestmentPerformanceDTO(
                    inv.getTicker(), inv.getType(),
                    pnlPercentForDTO,
                    currentPriceForDTO,
                    holdingValueForDTO
            );
        }

        if (individualPnlPercent.compareTo(aggregators.minPnlPercent) < 0) {
            aggregators.minPnlPercent = individualPnlPercent;
            aggregators.worstPerformer = new InvestmentPerformanceDTO(
                    inv.getTicker(), inv.getType(),
                    pnlPercentForDTO,
                    currentPriceForDTO,
                    holdingValueForDTO
            );
        }
    }

    private BigDecimal calculatePnlPercentage(BigDecimal pnlAbsolute, BigDecimal costBasis) {
        if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
            return pnlAbsolute
                    .divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else if (pnlAbsolute.compareTo(BigDecimal.ZERO) > 0) {
            return new BigDecimal(Integer.MAX_VALUE);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateRealizedPnlInPreferredCurrency(List<Investment> soldInvestments, String preferredCurrency) {
        BigDecimal realizedPnlPreferred = BigDecimal.ZERO;
        for (Investment inv : soldInvestments) {
            if (inv.getSellPrice() != null && inv.getPurchasePrice() != null && inv.getAmount() != null) {
                BigDecimal gainLossPerUnit = inv.getSellPrice().subtract(inv.getPurchasePrice());
                BigDecimal pnlAbs = gainLossPerUnit.multiply(inv.getAmount());

                String srcCurrency = inv.getCurrency() != null ? inv.getCurrency().toUpperCase() : preferredCurrency;
                if (!srcCurrency.equalsIgnoreCase(preferredCurrency)) {
                    BigDecimal converted = currencyConversionService.convert(pnlAbs, srcCurrency, preferredCurrency);
                    if (converted != null) {
                        pnlAbs = converted;
                    } else {
                        logger.warn("Failed to convert realized PnL from {} to {} for investment {}", srcCurrency, preferredCurrency, inv.getId());
                    }
                }
                realizedPnlPreferred = realizedPnlPreferred.add(pnlAbs);
            }
        }
        return realizedPnlPreferred;
    }

    private Map<String, BigDecimal> calculateAssetAllocation(Map<String, BigDecimal> valueByType, BigDecimal totalValue) {
        Map<String, BigDecimal> assetAllocationPercentage = new HashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : valueByType.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                assetAllocationPercentage.put(entry.getKey(), percentage.setScale(2, RoundingMode.HALF_UP));
            }
        }
        return assetAllocationPercentage;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));
    }

    private void checkPortfolioOwnership(Portfolio portfolio, User user, String action) {
        if (!portfolio.getUser().getId().equals(user.getId())) {
            logger.warn("User '{}' (ID: {}) attempted to {} portfolio '{}' (ID: {}) owned by user '{}' (ID: {}). Access denied.",
                    user.getUsername(), user.getId(),
                    action,
                    portfolio.getName(), portfolio.getId(),
                    portfolio.getUser().getUsername(), portfolio.getUser().getId());
            throw new AccessDeniedException(String.format("User does not have permission to %s this portfolio.", action));
        }
    }

    private PortfolioSummaryResponse createEmptySummaryResponse() {
        PortfolioSummaryResponse.SummaryMetrics metrics = new PortfolioSummaryResponse.SummaryMetrics(
                "All Portfolios (None Found)",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyMap(),
                Collections.emptyMap(),
                0, null, null
        );
        return new PortfolioSummaryResponse(metrics, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    @Transactional
    public void updateAllActiveInvestmentValuesForUser(User user) {
        if (user == null) {
            logger.error("User object is null. Cannot update active investment values.");
            return;
        }
        List<Portfolio> userPortfolios = portfolioRepository.findByUserIdWithInvestments(user.getId());

        logger.info("Starting update of all active investment values for user: {}", user.getUsername());
        int portfoliosSavedCount = 0;
        int updatedInvestmentsCount = 0;

        for (Portfolio portfolio : userPortfolios) {
            PortfolioUpdateResults results = processPortfolioInvestmentsUpdate(portfolio, user);
            updatedInvestmentsCount += results.updatedInvestmentsCount();
            if (results.wasPortfolioSaved()) {
                portfoliosSavedCount++;
            }
        }
        logger.info("Finished update processing for user {}. Updated {} investments. Saved {} portfolios.",
                user.getUsername(), updatedInvestmentsCount, portfoliosSavedCount);

        // Always record the portfolio value history when refreshing values
        recordUserTotalPortfolioValue(user);
    }

    private PortfolioUpdateResults processPortfolioInvestmentsUpdate(Portfolio portfolio, User user) {
        logger.debug("Processing portfolio: {} (ID: {}) for user {}", portfolio.getName(), portfolio.getId(), user.getUsername());
        
        InvestmentUpdateResult investmentUpdateResult = updateActiveInvestmentPricesInPortfolio(portfolio);
        int investmentsSuccessfullyUpdatedInThisPortfolio = investmentUpdateResult.updatedCount();
        boolean anyIndividualInvestmentUpdated = investmentUpdateResult.anyUpdateOccurred();

        BigDecimal oldTotalValue = portfolio.getTotalValue();
        BigDecimal newTotalValue = calculatePortfolioValueInPreferredCurrency(portfolio, user);
        boolean portfolioTotalValueChanged = false;

        if (newTotalValue != null) {
            if (oldTotalValue == null || oldTotalValue.compareTo(newTotalValue) != 0) {
                portfolio.setTotalValue(newTotalValue);
                portfolioTotalValueChanged = true;
                logger.info("Portfolio {} (ID: {}) total value changed from {} to {}.", portfolio.getName(), portfolio.getId(), oldTotalValue, newTotalValue);
            } else {
                 logger.info("Portfolio {} (ID: {}) recalculated total value {} is same as stored value. No change to portfolio.totalValue.", portfolio.getName(), portfolio.getId(), newTotalValue);
            }
        } else {
            logger.error("Failed to recalculate total value for portfolio {} (ID: {}). The existing total value ({}) will be kept.", portfolio.getName(), portfolio.getId(), oldTotalValue);
        }

        boolean portfolioSaved = false;
        if (anyIndividualInvestmentUpdated || portfolioTotalValueChanged) {
            try {
                portfolioRepository.saveAndFlush(portfolio);
                portfolioSaved = true;
                logger.info("Portfolio {} (ID: {}) saved. anyIndividualInvestmentUpdated: {}, portfolioTotalValueChanged: {}", portfolio.getName(), portfolio.getId(), anyIndividualInvestmentUpdated, portfolioTotalValueChanged);
            } catch (Exception e) {
                logger.error("Failed to save portfolio {} (ID: {}) after updates. Error: {}", portfolio.getName(), portfolio.getId(), e.getMessage(), e);
            }
        }
        return new PortfolioUpdateResults(investmentsSuccessfullyUpdatedInThisPortfolio, portfolioSaved);
    }

    private InvestmentUpdateResult updateActiveInvestmentPricesInPortfolio(Portfolio portfolio) {
        int updatedCount = 0;
        boolean anyUpdateOccurred = false;
        for (Investment investment : portfolio.getInvestments()) {
            if (StatusInvestment.ACTIVE.equals(investment.getStatus()) &&
                investment.getTicker() != null && investment.getType() != null && investment.getCurrency() != null) {
                
                boolean updated = updateSingleActiveInvestmentPrice(investment, portfolio.getId());
                if (updated) {
                    updatedCount++;
                    anyUpdateOccurred = true;
                }
            }
        }
        return new InvestmentUpdateResult(updatedCount, anyUpdateOccurred);
    }

    private boolean updateSingleActiveInvestmentPrice(Investment investment, UUID portfolioId) {
        if ("Other".equalsIgnoreCase(investment.getType())) {
            logger.info("Skipping automatic price update for active investment ID {} in portfolio {} because its type is 'Other'.", investment.getId(), portfolioId);
            return false; // No update occurred for this 'Other' type investment
        }
        try {
            PriceInfo priceInfo = marketDataService.getCurrentValue(
                    investment.getTicker(), investment.getType(), investment.getCurrency(), true); // Force refresh

            if (priceInfo != null && priceInfo.value() != null) {
                if (investment.getCurrentValue() == null || investment.getCurrentValue().compareTo(priceInfo.value()) != 0) {
                    investment.setCurrentValue(priceInfo.value());
                    investment.setLastUpdateDate(LocalDateTime.now());
                    logger.debug("Updated investment {} (Ticker: {}) in portfolio {} to new value {}", 
                                 investment.getId(), investment.getTicker(), portfolioId, priceInfo.value());
                    return true; // Investment was updated
                }
                // Price is same, no update needed
                return false;
            } else {
                logger.warn("Failed to retrieve PriceInfo for active investment {} (Ticker: {}) in portfolio {}. Value not updated.",
                        investment.getId(), investment.getTicker(), portfolioId);
                return false; // PriceInfo not valid
            }
        } catch (IOException e) {
            logger.error("IOException updating active investment {} (Ticker: {}) in portfolio {}: {}",
                    investment.getId(), investment.getTicker(), portfolioId, e.getMessage());
            return false; // Error occurred
        } catch (Exception e) {
            logger.error("Unexpected error updating active investment {} (Ticker: {}) in portfolio {}: {}",
                    investment.getId(), investment.getTicker(), portfolioId, e.getMessage(), e);
            return false; // Error occurred
        }
    }

    @Override
    @Transactional
    public void updateAllActiveInvestmentValuesForCurrentUser() {
        User currentUser = getCurrentUser();
        updateAllActiveInvestmentValuesForUser(currentUser);
    }

    @Override
    @Transactional
    public void scheduledUpdateAllUsersPortfolioValues() {
        logger.info("Starting scheduled job: UpdateAllUsersPortfolioValues");
        List<User> allUsers = userRepository.findAll();
        int usersProcessed = 0;
        int usersFailed = 0;

        for (User user : allUsers) {
            try {
                logger.info("Scheduled update: Processing user {}", user.getUsername());
                updateAllActiveInvestmentValuesForUser(user);
                usersProcessed++;
            } catch (Exception e) {
                logger.error("Scheduled update: Failed to process user {} (ID: {}). Error: {}", user.getUsername(), user.getId(), e.getMessage(), e);
                usersFailed++;
            }
        }
        logger.info("Finished scheduled job: UpdateAllUsersPortfolioValues. Processed {} users, {} failures.", usersProcessed, usersFailed);
    }

    private void recordUserTotalPortfolioValue(User user) {
        // Ensure any pending changes are flushed before we calculate totals
        try {
            entityManager.flush();
        } catch (Exception e) {
            logger.warn("EntityManager flush failed before recording portfolio value history for user {}: {}", user.getUsername(), e.getMessage());
        }

        List<Portfolio> allUserPortfolios = portfolioRepository.findByUserId(user.getId());
        
        BigDecimal grandTotalValue = allUserPortfolios.stream()
                .map(Portfolio::getTotalValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        allUserPortfolios.stream()
                .filter(p -> p.getTotalValue() == null)
                .forEach(p -> logger.warn("Portfolio {} for user {} has null totalValue, not included in grand total history calculation.", p.getId(), user.getUsername()));

        String preferredCurrencyHist = getUserPreferredCurrency(user);

        UserPortfolioValueHistory historyRecord = new UserPortfolioValueHistory(
                user,
                LocalDateTime.now(),
                grandTotalValue.setScale(2, RoundingMode.HALF_UP),
                preferredCurrencyHist
        );
        userPortfolioValueHistoryRepository.save(historyRecord);
        logger.info("Recorded total portfolio value history for user {}: {} {}", user.getUsername(), historyRecord.getTotalValue(), historyRecord.getPreferredCurrency());
    }

    private String getUserPreferredCurrency(User user) {
        if (user.getSettings() == null || user.getSettings().isEmpty()) return "USD";
        return user.getSettings().stream()
                .filter(s -> "preferredCurrency".equals(s.getKey()))
                .map(UserSetting::getValue)
                .findFirst()
                .orElse("USD");
    }

    @Override
    public List<HistoricalDataPointDTO> getOverallUserValueHistory(String range) {
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        
        if ("all".equalsIgnoreCase(range)) {
            return userPortfolioValueHistoryRepository.findByUserIdOrderByTimestampAsc(currentUser.getId())
                    .stream()
                    .map(history -> new HistoricalDataPointDTO(history.getTimestamp(), history.getTotalValue()))
                    .toList();
        }

        LocalDateTime startDate = calculateStartDateForHistory(range, now);

        return userPortfolioValueHistoryRepository.findByUserIdAndTimestampGreaterThanEqualOrderByTimestampAsc(currentUser.getId(), startDate)
                .stream()
                .map(history -> new HistoricalDataPointDTO(history.getTimestamp(), history.getTotalValue()))
                .toList();
    }

    private LocalDateTime calculateStartDateForHistory(String range, LocalDateTime now) {
        Map<String, Supplier<LocalDateTime>> rangeCalculators = new HashMap<>();
        rangeCalculators.put("7d", () -> now.minusDays(7));
        rangeCalculators.put("1m", () -> now.minusMonths(1));
        rangeCalculators.put("3m", () -> now.minusMonths(3));
        rangeCalculators.put("6m", () -> now.minusMonths(6));
        rangeCalculators.put("1y", () -> now.minusYears(1));

        Supplier<LocalDateTime> calculator = rangeCalculators.get(range.toLowerCase());
        if (calculator != null) {
            return calculator.get();
        } else {
            logger.warn("Invalid range '{}' provided for history, defaulting to 1 month.", range);
            return now.minusMonths(1);
        }
    }
}


