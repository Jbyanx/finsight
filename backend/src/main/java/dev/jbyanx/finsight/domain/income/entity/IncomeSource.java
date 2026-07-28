package dev.jbyanx.finsight.domain.income.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "income_sources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomeType type;

    @Column(precision = 15, scale = 2)
    private BigDecimal estimatedAmount;

    @Enumerated(EnumType.STRING)
    private IncomeFrequency frequency;

    private Integer occurrencesPerMonth; // cuántas veces ocurre este ingreso/gasto al mes

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }

    public enum IncomeType {
        FIXED, VARIABLE
    }

    public enum IncomeFrequency {
        PER_SHIFT, WEEKLY, BIWEEKLY, MONTHLY, ONE_TIME
    }
}