package com.myapp.investment_dashboard_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "financial_profiles")
@Data
@NoArgsConstructor
public class FinancialProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonBackReference
    private User user;

    @Column(name = "monthly_income", precision = 19, scale = 2, nullable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "monthly_expenses", precision = 19, scale = 2, nullable = false)
    private BigDecimal monthlyExpenses;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @OneToMany(mappedBy = "financialProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FinancialGoal> goals = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public BigDecimal getMonthlySavings() {
        return monthlyIncome.subtract(monthlyExpenses);
    }
}