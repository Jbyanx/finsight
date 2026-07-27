package dev.jbyanx.finsight.domain.debt.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_debts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedDebt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String institution;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    private BigDecimal interestRate; // opcional, para cálculos avanzados

    private Integer paymentDueDay;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        // Si no se especifica saldo restante, asume que es el total
        if (remainingBalance == null) remainingBalance = totalAmount;
    }

    // Lógica de negocio en la entidad
    public BigDecimal getPaidAmount() {
        return totalAmount.subtract(remainingBalance);
    }

    public BigDecimal getProgressPercentage() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getPaidAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean isPaidOff() {
        return remainingBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    public void applyPayment(BigDecimal amount) {
        if (amount.compareTo(remainingBalance) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount exceeds remaining balance: " + remainingBalance);
        }
        remainingBalance = remainingBalance.subtract(amount);
        if (isPaidOff()) active = false; // se desactiva automáticamente al saldar
    }
}