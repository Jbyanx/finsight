package dev.jbyanx.finsight.domain.savings.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "savings_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String institution;

    @Column(length = 100)
    private String nickname;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean isShared;

    @Column(length = 100)
    private String sharedOwnerName;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "savingsAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pocket> pockets = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        if (balance == null) balance = BigDecimal.ZERO;
    }

    public BigDecimal getAvailableBalance() {
        BigDecimal pocketsTotal = pockets.stream()
                .filter(Pocket::isActive)
                .map(Pocket::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance.subtract(pocketsTotal);
    }
}