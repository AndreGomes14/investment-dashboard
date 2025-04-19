package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExternalApiCacheRepository externalApiCacheRepository;
    private static final Logger logger = LoggerFactory.getLogger(InvestmentService.class);

    @Autowired
    public InvestmentService(InvestmentRepository investmentRepository, RestTemplate restTemplate, ObjectMapper objectMapper, ExternalApiCacheRepository externalApiCacheRepository) {
        this.investmentRepository = investmentRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.externalApiCacheRepository = externalApiCacheRepository;
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
     *
     * @param investment The investment object to create.
     * @return The created investment.
     */
    public Investment createInvestment(Investment investment) {
        investment.setLastUpdateDate(LocalDateTime.now()); // Set initial last update
        return investmentRepository.save(investment);
    }

    /**
     * Updates an existing investment.
     *
     * @param id           The ID of the investment to update.
     * @param updatedInvestment The updated investment object.
     * @return The updated investment, or null if the investment does not exist.
     */
    @Transactional
    public Investment updateInvestment(UUID id, Investment updatedInvestment) {
        Optional<Investment> existingInvestment = investmentRepository.findById(id);
        if (existingInvestment.isPresent()) {
            Investment investment = existingInvestment.get();
            investment.setTicker(updatedInvestment.getTicker());
            investment.setType(updatedInvestment.getType());
            investment.setAmount(updatedInvestment.getAmount());
            investment.setPurchasePrice(updatedInvestment.getPurchasePrice());
            investment.setLastUpdateDate(LocalDateTime.now()); // update
            investment.setStatus(updatedInvestment.getStatus());
            return investmentRepository.save(investment);
        }
        return null;
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
     * Gets the current value of the investment.
     *
     * @param investmentId
     * @return
     */
    public BigDecimal getInvestmentCurrentValue(UUID investmentId) {
        Optional<Investment> investmentOptional = investmentRepository.findById(investmentId);
        if (investmentOptional.isPresent()) {
            Investment investment = investmentOptional.get();
            try {
                return getCurrentValue(investment.getTicker(), investment.getType());
            } catch (IOException e) {
                logger.error("Error fetching current value for investment {}: {}", investmentId, e.getMessage());
                return null;
            }

        }
        return null;
    }

    /**
     * Updates the current value of an investment.
     *
     * @param id The ID of the investment to update.
     * @return The updated investment, or null if the investment does not exist or the update fails.
     */
    @Transactional
    public Investment updateInvestmentValue(UUID id) {
        Optional<Investment> investmentOptional = investmentRepository.findById(id);
        if (investmentOptional.isPresent()) {
            Investment investment = investmentOptional.get();
            try {
                BigDecimal currentValue = getCurrentValue(investment.getTicker(), investment.getType());
                if (currentValue != null) {
                    investment.setCurrentValue(currentValue);
                    investment.setLastUpdateDate(LocalDateTime.now());
                    return investmentRepository.save(investment);
                }
            } catch (IOException e) {
                logger.error("Error updating current value for investment {}: {}", id, e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Retrieves the current value of a financial instrument (stock, ETF, crypto)
     * from an external API.  Currently supports Yahoo Finance for stocks/ETFs
     * and CoinGecko for cryptocurrencies.
     *
     * @param ticker The ticker symbol of the financial instrument.
     * @param type   The type of asset (e.g., "stock", "crypto").
     * @return The current value of the instrument, or null if the value cannot be retrieved.
     * @throws IOException if there is an error parsing the JSON response.
     */
    private BigDecimal getCurrentValue(String ticker, String type) throws IOException {
        // First, check the cache
        Optional<ExternalApiCache> cacheEntry = externalApiCacheRepository.findByTickerAndType(ticker, type);
        if (cacheEntry.isPresent()) {
            ExternalApiCache cache = cacheEntry.get();
            // Only return the cached value if it's recent enough
            if (cache.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
                return cache.getCurrentValue();
            }
            // Remove the outdated cache entry
            externalApiCacheRepository.delete(cache);
        }

        // If not in cache or outdated, fetch from external API
        BigDecimal currentValue = fetchCurrentValueFromAPI(ticker, type);
        if (currentValue != null) {
            // Update the cache
            ExternalApiCache newCacheEntry = new ExternalApiCache();
            newCacheEntry.setTicker(ticker);
            newCacheEntry.setType(type);
            newCacheEntry.setCurrentValue(currentValue);
            newCacheEntry.setLastUpdated(LocalDateTime.now());
            externalApiCacheRepository.save(newCacheEntry);
        }
        return currentValue;
    }

    private BigDecimal fetchCurrentValueFromAPI(String ticker, String type) throws IOException {
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
                if (indicatorsNode.isMissingNode()) {
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
}