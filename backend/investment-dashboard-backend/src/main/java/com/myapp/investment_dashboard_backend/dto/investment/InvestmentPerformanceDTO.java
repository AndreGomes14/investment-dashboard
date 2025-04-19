package com.myapp.investment_dashboard_backend.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPerformanceDTO {
    private UUID id;
    private String ticker;
    private String type;
    private BigDecimal performance;
}
