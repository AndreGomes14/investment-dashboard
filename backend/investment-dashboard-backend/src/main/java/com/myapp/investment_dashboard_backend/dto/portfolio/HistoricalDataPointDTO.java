package com.myapp.investment_dashboard_backend.dto.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricalDataPointDTO(
    LocalDateTime timestamp,
    BigDecimal value
) {} 