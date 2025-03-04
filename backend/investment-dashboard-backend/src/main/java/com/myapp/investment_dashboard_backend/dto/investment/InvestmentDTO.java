package com.myapp.investment_dashboard_backend.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentDTO {
    private Long id;
    private Long portfolioId;
    private String ticker;
    private String type;
    private BigDecimal amount;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private BigDecimal performance;
    private LocalDateTime lastUpdateDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
