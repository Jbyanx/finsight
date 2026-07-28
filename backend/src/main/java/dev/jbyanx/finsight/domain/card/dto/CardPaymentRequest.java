package dev.jbyanx.finsight.domain.card.dto;

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
public class CardPaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Fuente del dinero (opcional). Si es null, el pago solo se registra
     * contablemente en la tarjeta: alguien más lo pagó fuera de la app —
     * misma idea que en FixedDebt. Solo se aceptan CASH y SAVINGS; pagar
     * una tarjeta con otra tarjeta mueve la deuda de lugar, no la salda.
     */
    private OriginType originType;

    /** Requerido cuando originType = SAVINGS. */
    private String savingsAccountId;

    /** Bolsillo del que sale el dinero (opcional, solo con originType = SAVINGS). */
    private String pocketId;
}