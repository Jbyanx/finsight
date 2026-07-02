package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool de LECTURA de deudas fijas. Mismo patrón de seguridad (userId del contexto).
 */
@Component
@RequiredArgsConstructor
public class DebtTools {

    private final FixedDebtService fixedDebtService;

    @Tool(description = "Lista las deudas fijas del usuario con su saldo restante, "
            + "cuota mensual, día de pago y progreso. Úsala cuando el usuario "
            + "pregunte por sus deudas o cuánto le falta por pagar.")
    public String listDebts(ToolContext toolContext) {
        String userId = (String) toolContext.getContext().get(ToolContextKeys.USER_ID);
        if (userId == null) return "No se pudo identificar al usuario.";

        List<FixedDebtResponse> debts = fixedDebtService.getAllByUser(userId);
        if (debts.isEmpty()) {
            return "El usuario no tiene deudas fijas registradas.";
        }

        StringBuilder sb = new StringBuilder("Deudas fijas del usuario:\n");
        for (FixedDebtResponse d : debts) {
            sb.append("- ").append(d.getInstitution());
            if (d.getDescription() != null && !d.getDescription().isBlank()) {
                sb.append(" (").append(d.getDescription()).append(")");
            }
            sb.append(": saldo restante ").append(d.getRemainingBalance())
                    .append(" de ").append(d.getTotalAmount())
                    .append(", cuota mensual ").append(d.getMonthlyPayment())
                    .append(", paga día ").append(d.getPaymentDueDay())
                    .append(", progreso ").append(d.getProgressPercentage()).append("%");
            if (d.isPaidOff()) sb.append(" — SALDADA");
            sb.append("\n");
        }
        return sb.toString();
    }
}