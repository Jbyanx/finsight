package dev.jbyanx.finsight.domain.ai.pending;

import dev.jbyanx.finsight.domain.debt.dto.DebtPaymentRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ejecuta una {@link PendingAction} ya confirmada, contra el service real.
 *
 * SEGURIDAD: esta es la ÚNICA vía por la que una mutación disparada desde el bot
 * llega a la BD, y la invoca el CÓDIGO (ConfirmationHandler) tras detectar una
 * confirmación explícita — NUNCA el modelo. El LLM solo propone (preview); nunca
 * tiene una tool que ejecute directo.
 *
 * Cada type de acción se resuelve acá. Agregar una nueva acción de escritura =
 * un case nuevo + su tool de preview. El patrón de seguridad no cambia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingActionExecutor {

    public static final String PAY_DEBT = "PAY_DEBT";

    private final FixedDebtService fixedDebtService;

    /**
     * @return mensaje de resultado para mostrar al usuario tras ejecutar.
     */
    public String execute(String userId, PendingAction action) {
        return switch (action.getType()) {
            case PAY_DEBT -> payDebt(userId, action);
            default -> {
                log.warn("Unknown pending action type: {}", action.getType());
                yield "No pude completar la acción; intentá de nuevo.";
            }
        };
    }

    private String payDebt(String userId, PendingAction action) {
        String debtId = action.param("debtId");
        BigDecimal amount = new BigDecimal(action.param("amount"));

        FixedDebtResponse result = fixedDebtService.applyPayment(
                userId, debtId, DebtPaymentRequest.builder().amount(amount).build());

        if (result.isPaidOff()) {
            return "¡Listo! Pagaste %s y saldaste por completo la deuda de %s. 🎉"
                    .formatted(amount, result.getInstitution());
        }
        return "¡Listo! Registré el pago de %s. Te queda un saldo de %s en la deuda de %s (próxima cuota el día %d)."
                .formatted(amount, result.getRemainingBalance(),
                        result.getInstitution(), result.getPaymentDueDay());
    }
}