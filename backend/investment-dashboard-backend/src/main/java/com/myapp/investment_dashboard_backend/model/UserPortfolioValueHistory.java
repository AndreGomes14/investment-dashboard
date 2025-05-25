package com.myapp.investment_dashboard_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_portfolio_value_history")
@Data
@NoArgsConstructor
public class UserPortfolioValueHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "total_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalValue;

    @Column(name = "preferred_currency", length = 3, nullable = false)
    private String preferredCurrency;

    public UserPortfolioValueHistory(User user, LocalDateTime timestamp, BigDecimal totalValue, String preferredCurrency) {
        this.user = user;
        this.timestamp = timestamp;
        this.totalValue = totalValue;
        this.preferredCurrency = preferredCurrency;
    }
} 