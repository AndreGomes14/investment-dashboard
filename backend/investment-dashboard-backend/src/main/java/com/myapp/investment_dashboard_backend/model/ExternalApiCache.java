package com.myapp.investment_dashboard_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_api_cache", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker", "type"})
})
@Data
@NoArgsConstructor
public class ExternalApiCache {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "current_value", precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "currency", length = 3)
    private String currency;
}