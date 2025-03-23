package com.myapp.investment_dashboard_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for Yahoo Finance API settings
 */
@Configuration
public class YahooFinanceConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    // Default timeout settings in milliseconds
    public static final int CONNECTION_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 10000;
    
    // Yahoo Finance API settings
    public static final String YAHOO_FINANCE_API_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    public static final String YAHOO_FINANCE_API_QUERY_PARAMS = "?interval=1d&range=1d";
} 