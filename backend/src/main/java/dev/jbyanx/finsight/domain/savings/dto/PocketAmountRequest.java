package dev.jbyanx.finsight.domain.savings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Monto para operar un bolsillo (depositar / retirar).
 * Experiencia tipo Nu: el usuario piensa en "agregar/sacar $X", no en el total final.
 */
@Getter
@NoArgsConstructor
public class PocketAmountRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0", inclusive = false, message = "Amount must be greater than zero")
    private BigDecimal amount;
}
