package dev.jbyanx.finsight.domain.card.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCard {

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
    private BigDecimal creditLimit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal usedAmount;

    @Column(nullable = false)
    private Integer cutOffDay;

    @Column(nullable = false)
    private Integer paymentDueDay;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        if (usedAmount == null) usedAmount = BigDecimal.ZERO;
    }

    // Lógica de negocio en la entidad
    public BigDecimal getUtilizationPercentage() {
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return usedAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(creditLimit, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean isYellowAlert() {
        return getUtilizationPercentage().compareTo(BigDecimal.valueOf(25)) >= 0;
    }

    public boolean isRedAlert() {
        return getUtilizationPercentage().compareTo(BigDecimal.valueOf(30)) >= 0;
    }
}