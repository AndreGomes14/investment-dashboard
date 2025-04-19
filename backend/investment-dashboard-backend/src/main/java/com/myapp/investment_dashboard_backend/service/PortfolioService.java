package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import com.myapp.investment_dashboard_backend.repository.PortfolioRepository;
import com.myapp.investment_dashboard_backend.repository.ExternalApiCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ExternalApiCacheRepository externalApiCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);

    @Autowired
    public PortfolioService(PortfolioRepository portfolioRepository, ExternalApiCacheRepository externalApiCacheRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.portfolioRepository = portfolioRepository;
        this.externalApiCacheRepository = externalApiCacheRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

    public Portfolio createPortfolio(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    /**
     * Calculates the total value of a portfolio based on its investments.
     * This method fetches the current price of each investment, multiplies it
     * by the quantity held, and sums the values.
     *
     * @param portfolio The portfolio for which to calculate the total value.
     * @return The total value of the portfolio, or null in case of error.
     */
    public BigDecimal calculatePortfolioValue(Portfolio portfolio) {
        BigDecimal totalValue = BigDecimal.ZERO;
        if (portfolio == null || portfolio.getInvestments() == null) {
            return totalValue;
        }

        for (Investment investment : portfolio.getInvestments()) {
            try {
                BigDecimal currentValue = getCurrentValue(investment.getTicker(), investment.getType()); // Use getType()
                if (currentValue != null) {
                    BigDecimal investmentValue = currentValue.multiply(new BigDecimal(String.valueOf(investment.getAmount()))); // Use amount
                    totalValue = totalValue.add(investmentValue);
                } else {
                    logger.warn("Could not retrieve current value for {} - {}. Skipping.", investment.getType(), investment.getTicker());
                    return null;
                }
            } catch (Exception e) {
                logger.error("Error calculating value for investment {} in portfolio {}: {}", investment.getId(), portfolio.getId(), e.getMessage());
                return null; // Return null in case of error.
            }
        }
        return totalValue.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Retrieves the current value of a financial instrument (stock, ETF, crypto)
     * from an external API or the local cache.  Currently supports Yahoo Finance
     * for stocks/ETFs and CoinGecko for cryptocurrencies.
     *
     * @param ticker The ticker symbol of the financial instrument.
     * @param type   The type of asset (e.g., "stock", "crypto").  Use the field name "type"
     * @return The current value of the instrument, or null if the value cannot be retrieved.
     * @throws IOException if there is an error parsing the JSON response.
     */
    private BigDecimal getCurrentValue(String ticker, String type) throws IOException {
        // First, check the cache
        Optional<ExternalApiCache> cacheEntry = externalApiCacheRepository.findByTickerAndType(ticker, type); // Use type
        if (cacheEntry.isPresent()) {
            ExternalApiCache cache = cacheEntry.get();
            // Only return the cached value if it's recent enough (e.g., within 5 minutes)
            if (cache.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
                return cache.getCurrentValue();
            }
            // Remove the outdated cache entry
            externalApiCacheRepository.delete(cache);
        }

        // If not in cache or outdated, fetch from external API
        BigDecimal currentValue = fetchCurrentValueFromAPI(ticker, type); // Use type
        if (currentValue != null) {
            // Update the cache
            ExternalApiCache newCacheEntry = new ExternalApiCache();
            newCacheEntry.setTicker(ticker);
            newCacheEntry.setType(type); // Use type
            newCacheEntry.setCurrentValue(currentValue);
            newCacheEntry.setLastUpdated(LocalDateTime.now());
            externalApiCacheRepository.save(newCacheEntry);
        }
        return currentValue;
    }

    /**
     * Fetches the current value of a financial instrument from the external API.
     * This method should not be called directly, but only via getCurrentValue
     * @param ticker
     * @param type
     * @return
     * @throws IOException
     */
    private BigDecimal fetchCurrentValueFromAPI(String ticker, String type) throws IOException{
        String url = null;
        JsonNode root = null;

        try {
            if ("stock".equalsIgnoreCase(type) || "etf".equalsIgnoreCase(type)) {
                // Yahoo Finance API URL
                url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker;
                String response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);
                // Navigate the JSON structure to find the price.
                JsonNode resultNode = root.path("chart").path("result").get(0);
                if (resultNode.isMissingNode()) {
                    logger.warn("Invalid response structure from Yahoo Finance for ticker: {}", ticker);
                    return null;
                }
                JsonNode indicatorsNode = resultNode.path("indicators");
                if(indicatorsNode.isMissingNode()){
                    logger.warn("Invalid response structure from Yahoo Finance for ticker: {}", ticker);
                    return null;
                }
                JsonNode quoteNode = indicatorsNode.path("quote").get(0);

                if (quoteNode.isMissingNode()) {
                    logger.warn("Invalid response structure from Yahoo Finance for ticker: {}", ticker);
                    return null;
                }
                JsonNode closeNode = quoteNode.path("close").get(0);
                if (closeNode.isMissingNode() || closeNode.isNull()) {
                    logger.warn("Invalid response structure from Yahoo Finance for ticker: {}", ticker);
                    return null;
                }
                double price = closeNode.asDouble();
                return BigDecimal.valueOf(price);
            } else if ("crypto".equalsIgnoreCase(type)) {
                // CoinGecko API URL
                url = "https://api.coingecko.com/api/v3/simple/price?ids=" + ticker + "&vs_currencies=usd";
                String response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);
                // CoinGecko returns a JSON object with the ticker as the key.
                JsonNode priceNode = root.path(ticker.toLowerCase()).path("usd");
                if (priceNode.isMissingNode()) {
                    logger.warn("Could not find price for crypto ticker: {} in CoinGecko response", ticker);
                    return null; // Handle the case where the ticker is not found
                }
                double price = priceNode.asDouble();
                return BigDecimal.valueOf(price);
            } else {
                logger.warn("Unsupported asset type: {}", type);
                return null;
            }
        } catch (IOException e) {
            logger.error("Error fetching/parsing data for {} from {}: {}", ticker, url, e.getMessage());
            throw e; // Re-throw the exception so it's handled in the controller.
        } catch (Exception e) {
            logger.error("Unexpected error fetching data for {} from {}: {}", ticker, url, e.getMessage());
            return null;
        }
    }

    /**
     * Updates the values of all investments in a portfolio and recalculates the
     * portfolio value.
     *
     * @param portfolioId The ID of the portfolio to update.
     * @return The updated portfolio with the new total value, or null if the update fails.
     */
    @Transactional // Add transactional annotation
    public Portfolio updatePortfolioValues(UUID portfolioId) {
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByIdWithInvestments(portfolioId);
        if (portfolioOptional.isPresent()) {
            Portfolio portfolio = portfolioOptional.get();
            BigDecimal newTotalValue = calculatePortfolioValue(portfolio);
            if (newTotalValue != null) {
                portfolio.setTotalValue(newTotalValue);
                for (Investment investment : portfolio.getInvestments()) {
                    try {
                        BigDecimal currentValue = getCurrentValue(investment.getTicker(), investment.getType()); // Use type
                        if (currentValue != null) {
                            investment.setCurrentValue(currentValue);
                            investment.setLastUpdateDate(LocalDateTime.now());
                        }
                    } catch (IOException e) {
                        logger.error("Error updating investment value: {}", e.getMessage());
                        return null; // Or handle the error as appropriate for your application
                    }
                }
                return portfolioRepository.save(portfolio);
            } else {
                return null;
            }
        }
        return null;
    }
}

