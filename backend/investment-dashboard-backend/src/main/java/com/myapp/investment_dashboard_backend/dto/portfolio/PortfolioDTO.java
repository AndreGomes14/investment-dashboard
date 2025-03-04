package com.myapp.investment_dashboard_backend.dto.portfolio;

import com.myapp.investment_dashboard_backend.dto.investment.InvestmentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {
    private Long id;
    private Long userId;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalValue;
    private BigDecimal totalPerformance;
    @Builder.Default
    private List<InvestmentDTO> investments = new ArrayList<>();
}
