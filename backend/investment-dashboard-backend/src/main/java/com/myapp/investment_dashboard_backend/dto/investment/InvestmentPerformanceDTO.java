package com.myapp.investment_dashboard_backend.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPerformanceDTO {
    private Long id;
    private String ticker;
    private String type;
    private BigDecimal performance;
}
