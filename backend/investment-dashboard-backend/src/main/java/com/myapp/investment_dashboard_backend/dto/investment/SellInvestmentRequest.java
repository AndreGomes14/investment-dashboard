package com.myapp.investment_dashboard_backend.dto.investment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SellInvestmentRequest(
        @NotNull(message = "Sell price cannot be null")
        @Positive(message = "Sell price must be positive")
        BigDecimal sellPrice
) {}
