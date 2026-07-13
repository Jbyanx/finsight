package dev.jbyanx.finsight.domain.transaction.entity;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OriginType originType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_account_id")
    private SavingsAccount savingsAccount;

    /**
     * Bolsillo del que sale el dinero (opcional, solo para origen SAVINGS).
     *
     * Cuando apunta a un bolsillo COMPARTIDO, el gasto se paga con dinero de un
     * tercero ("paga la factura con la plata de tu papá"): descuenta el saldo del
     * bolsillo y de la cuenta, pero NO afecta el patrimonio del usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pocket_id")
    private Pocket pocket;

    /**
     * SNAPSHOT: true si el gasto se pagó con dinero de un tercero.
     *
     * Es snapshot y no valor derivado A PROPÓSITO: el bolsillo puede dejar de ser
     * compartido más adelante, y eso no debe reescribir la historia contable.
     *
     * Los cálculos de "cuánto gasté yo" DEBEN excluir thirdParty = true.
     */
    @Column(nullable = false)
    private boolean thirdParty;

    /** Dueño del dinero cuando thirdParty = true. Ej: "Papá". */
    @Column(length = 100)
    private String thirdPartyOwnerName;

    @Column(nullable = false)
    private boolean shared;

    @Column(length = 100)
    private String sharedWithName;

    @Column(precision = 15, scale = 2)
    private BigDecimal sharedAmount;

    @Column
    private LocalDateTime paymentDeadline;

    @Column(nullable = false)
    private Integer installments;

    @Column(nullable = false)
    private Integer currentInstallment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (date == null) date = LocalDateTime.now();
        if (installments == null) installments = 1;
        if (currentInstallment == null) currentInstallment = 1;
        if (status == null) status = TransactionStatus.COMPLETED;
        if (!shared) {
            sharedWithName = null;
            sharedAmount = null;
            paymentDeadline = null;
        }
        if (!thirdParty) thirdPartyOwnerName = null;
    }

    public enum OriginType {
        CREDIT_CARD, SAVINGS, CASH
    }

    public enum TransactionStatus {
        PENDING, COMPLETED
    }
}