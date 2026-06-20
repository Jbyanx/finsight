package dev.jbyanx.finsight.domain.savings.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pockets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pocket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_account_id", nullable = false)
    private SavingsAccount savingsAccount;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(precision = 15, scale = 2)
    private BigDecimal goalAmount;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        if (balance == null) balance = BigDecimal.ZERO;
    }

    public BigDecimal getProgressPercentage() {
        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) == 0)
            return null;
        return balance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 2, java.math.RoundingMode.HALF_UP);
    }
}