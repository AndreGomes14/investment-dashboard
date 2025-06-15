package com.myapp.investment_dashboard_backend.dto.investment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvestmentRequest {

    private String ticker;
    private String name;

    @NotBlank(message = "Type is required")
    private String type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0001", message = "Purchase price must be greater than zero")
    private BigDecimal purchasePrice;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency;

    // Optional. For assets of type "Other" the user can supply the current value per unit.
    @DecimalMin(value = "0.0000", inclusive = false, message = "Current value must be positive")
    private BigDecimal currentValue;
}

