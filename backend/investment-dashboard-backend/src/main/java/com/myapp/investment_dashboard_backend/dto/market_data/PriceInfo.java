package com.myapp.investment_dashboard_backend.dto.market_data;

import java.math.BigDecimal;

/**
 * DTO to hold a fetched price and its currency.
 */
public record PriceInfo(
        BigDecimal value,
        String currency
) {}
