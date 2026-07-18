package dev.jbyanx.finsight.domain.debt.dto;

import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtPaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Fuente del dinero (opcional). Si es null el pago solo se registra contablemente
     * en la deuda: a veces paga un tercero desde una cuenta que no está en la app.
     * Solo se aceptan CASH y SAVINGS — pagar deuda con tarjeta es mover la deuda de
     * lugar, no saldarla.
     */
    private OriginType originType;

    /** Requerido cuando originType = SAVINGS. */
    private String savingsAccountId;

    /** Bolsillo del que sale el dinero (opcional, solo con originType = SAVINGS). */
    private String pocketId;
}