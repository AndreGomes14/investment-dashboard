package com.myapp.investment_dashboard_backend.dto.investment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellInvestmentRequest {
    @NotNull(message = "Sell price is required")
    @DecimalMin(value = "0.0001", message = "Sell price must be greater than zero")
    private BigDecimal sellPrice;

    private LocalDateTime sellDate;
}
