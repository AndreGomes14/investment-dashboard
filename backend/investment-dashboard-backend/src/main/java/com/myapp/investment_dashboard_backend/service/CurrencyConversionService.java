package com.myapp.investment_dashboard_backend.service;

import java.io.IOException;
import java.math.BigDecimal;

public interface CurrencyConversionService {
    BigDecimal convert(BigDecimal amount, String sourceCurrency, String targetCurrency);
    BigDecimal getConversionRate(String sourceCurrency, String targetCurrency) throws IOException;
    // Add the setSelf method to the interface if it's intended to be part of the public contract
    // or ensure the @Lazy injection target type in the Impl is the interface itself.
    // For now, keeping it out of the interface as it's an implementation detail for caching.
} 