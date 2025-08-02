package com.myapp.investment_dashboard_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.myapp.investment_dashboard_backend.utils.GoalStatus;
import com.myapp.investment_dashboard_backend.utils.GoalType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_goals")
@Data
@NoArgsConstructor
public class FinancialGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_profile_id", nullable = false)
    @JsonBackReference
    private FinancialProfile financialProfile;

    @Column(name = "goal_name", nullable = false, length = 100)
    private String goalName;

    @Column(name = "goal_description", length = 500)
    private String goalDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 20)
    private GoalType goalType;

    @Column(name = "target_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "current_saved", precision = 19, scale = 2, nullable = false)
    private BigDecimal currentSaved = BigDecimal.ZERO;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public BigDecimal getProgressPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentSaved.divide(targetAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Transient
    public BigDecimal getRemainingAmount() {
        return targetAmount.subtract(currentSaved);
    }
}