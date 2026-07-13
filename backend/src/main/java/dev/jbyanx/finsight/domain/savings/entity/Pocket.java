package dev.jbyanx.finsight.domain.savings.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bolsillo dentro de una cuenta de ahorro.
 *
 * MODELO DE PROPIEDAD (importante):
 * Un bolsillo puede ser COMPARTIDO: contiene dinero que NO es del usuario
 * (ej. la plata que el papá guarda en la cuenta Nequi del usuario).
 *
 * Reemplaza el modelo anterior, donde 'shared' vivía solo en la cuenta y era
 * todo-o-nada: marcar la cuenta como compartida borraba TODO su saldo de los
 * cálculos, incluido el dinero propio. En la vida real una cuenta propia aloja
 * porciones de terceros; se comparte la porción, no el contenedor.
 *
 * Distinción clave:
 *   - shared = true  -> el dinero NO es del usuario. Nunca suma a su patrimonio.
 *   - shared = false -> es del usuario, aunque esté reservado para una meta.
 */
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

    /** true = el dinero de este bolsillo es de un tercero, no del usuario. */
    @Column(nullable = false)
    private boolean shared;

    /** Nombre del dueño real cuando shared = true. Ej: "Papá", "Tía". */
    @Column(length = 100)
    private String sharedOwnerName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        if (balance == null) balance = BigDecimal.ZERO;
        if (!shared) sharedOwnerName = null;
    }

    /**
     * Progreso hacia la meta. Solo aplica a bolsillos propios: un bolsillo
     * compartido no es una meta de ahorro del usuario.
     */
    public BigDecimal getProgressPercentage() {
        if (shared || goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) == 0)
            return null;
        return balance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 2, java.math.RoundingMode.HALF_UP);
    }
}