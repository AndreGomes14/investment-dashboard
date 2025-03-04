package com.myapp.investment_dashboard_backend.dto.investment;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestmentRequest {
    private String ticker;
    private String type;

    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @DecimalMin(value = "0.0001", message = "Purchase price must be greater than zero")
    private BigDecimal purchasePrice;
}
