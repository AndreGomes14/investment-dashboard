package com.myapp.investment_dashboard_backend.model;

/**
 * Enum representing different types of investments
 */
public enum InvestmentType {
    STOCK,
    ETF,
    MUTUAL_FUND,
    BOND,
    CRYPTO,
    FOREX,
    COMMODITY,
    REAL_ESTATE,
    OTHER;
    
    /**
     * Check if an investment type is supported by Yahoo Finance
     * @param type The investment type to check
     * @return true if supported, false otherwise
     */
    public static boolean isYahooFinanceSupported(InvestmentType type) {
        return type == STOCK || type == ETF || type == MUTUAL_FUND || 
               type == BOND || type == FOREX || type == COMMODITY;
    }
} 