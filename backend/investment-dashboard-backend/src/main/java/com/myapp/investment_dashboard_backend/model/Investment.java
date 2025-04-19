package com.myapp.investment_dashboard_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "investments")
@Data
@NoArgsConstructor
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "purchase_price", precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    @Column(name = "current_value", precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, SOLD, DELETED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
