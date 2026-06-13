package dev.jbyanx.finsight.domain.income.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "income_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_source_id")
    private IncomeSource incomeSource;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationType destinationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_account_id")
    private SavingsAccount savingsAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pocket_id")
    private Pocket pocket;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }

    public enum DestinationType {
        CASH, SAVINGS, POCKET
    }
}