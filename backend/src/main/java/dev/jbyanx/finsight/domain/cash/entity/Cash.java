package dev.jbyanx.finsight.domain.cash.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cash {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void add(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void subtract(BigDecimal amount) {
        if (amount.compareTo(this.balance) > 0)
            throw new IllegalArgumentException("Insufficient cash balance");
        this.balance = this.balance.subtract(amount);
    }
}