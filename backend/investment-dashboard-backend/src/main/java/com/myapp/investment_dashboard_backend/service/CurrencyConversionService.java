package com.myapp.investment_dashboard_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CurrencyConversionService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyConversionService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${exchangerateapi.api.key}")
    private String exchangeRateApiKey;

    // Cache configuration name (must match configuration in CachingConfig.java or similar)
    public static final String EXCHANGE_RATE_CACHE = "exchangeRates";

    @Autowired
    public CurrencyConversionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Converts an amount from a source currency to a target currency using cached exchange rates.
     *
     * @param amount        The amount to convert.
     * @param sourceCurrency The 3-letter code of the source currency (e.g., "USD").
     * @param targetCurrency The 3-letter code of the target currency (e.g., "EUR").
     * @return The converted amount, or null if conversion fails.
     */
    public BigDecimal convert(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        if (amount == null || sourceCurrency == null || targetCurrency == null) {
            logger.warn("Conversion attempted with null amount, source, or target currency.");
            return null;
        }
        if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
            return amount; // No conversion needed
        }

        try {
            BigDecimal rate = getConversionRate(sourceCurrency.toUpperCase(), targetCurrency.toUpperCase());
            if (rate != null) {
                return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
            } else {
                logger.error("Could not retrieve conversion rate from {} to {}.", sourceCurrency, targetCurrency);
                return null;
            }
        } catch (IOException e) {
            logger.error("IOException while getting conversion rate from {} to {}: {}", sourceCurrency, targetCurrency, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during currency conversion from {} to {}: {}", sourceCurrency, targetCurrency, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches the conversion rate from the source currency to the target currency.
     * This method is cached based on source and target currency codes.
     *
     * @param sourceCurrency The source currency code (uppercase).
     * @param targetCurrency The target currency code (uppercase).
     * @return The conversion rate, or null if not found or error occurs.
     * @throws IOException If network or parsing errors occur.
     */
    @Cacheable(value = EXCHANGE_RATE_CACHE, key = "#sourceCurrency + '_' + #targetCurrency")
    public BigDecimal getConversionRate(String sourceCurrency, String targetCurrency) throws IOException {
        if (exchangeRateApiKey == null || exchangeRateApiKey.isEmpty() || "YOUR_EXCHANGERATEAPI_KEY".equals(exchangeRateApiKey)) {
            logger.error("ExchangeRate-API key is not configured properly in application.properties.");
            return null;
        }

        String url = String.format("https://v6.exchangerate-api.com/v6/%s/latest/%s",
                exchangeRateApiKey, sourceCurrency);
        logger.debug("Fetching exchange rates from: {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (!"success".equalsIgnoreCase(root.path("result").asText())) {
                String errorType = root.path("error-type").asText("unknown");
                logger.error("ExchangeRate-API error: {} for base currency {}. URL: {}", errorType, sourceCurrency, url);
                return null;
            }

            JsonNode ratesNode = root.path("conversion_rates");
            if (ratesNode.isMissingNode() || !ratesNode.isObject()) {
                logger.error("Invalid response structure from ExchangeRate-API: 'conversion_rates' node missing or not an object. URL: {}", url);
                return null;
            }

            JsonNode rateNode = ratesNode.path(targetCurrency);
            if (rateNode.isMissingNode() || !rateNode.isNumber()) {
                logger.error("Could not find conversion rate for target currency {} in response from ExchangeRate-API for base {}. URL: {}", targetCurrency, sourceCurrency, url);
                return null;
            }

            logger.info("Successfully retrieved exchange rate for {} -> {}: {}", sourceCurrency, targetCurrency, rateNode.decimalValue());
            return rateNode.decimalValue();

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching exchange rates for base {}: {} {}. URL: {}", sourceCurrency, e.getStatusCode(), e.getResponseBodyAsString(), url, e);
            return null;
        } catch (IOException e) {
            logger.error("IOException fetching/parsing exchange rates for base {}: {}. URL: {}", sourceCurrency, e.getMessage(), url, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching exchange rates for base {}: {}. URL: {}", sourceCurrency, e.getMessage(), url, e);
            return null;
        }
    }
}