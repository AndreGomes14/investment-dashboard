package com.myapp.investment_dashboard_backend.dto.api;

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
public class ExternalApiDataDTO {
    private String ticker;
    private String type;
    private BigDecimal currentValue;
    private LocalDateTime lastUpdated;
}
