package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import dev.jbyanx.finsight.domain.ai.pending.PendingActionExecutor;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tool de ESCRITURA para pagar deuda, en modo PREVIEW.
 *
 * NO ejecuta. Calcula la consecuencia real y deja una PendingAction en la
 * conversación. La confirmación se hace con BOTONES inline (✅/❌): cuando el
 * AiService detecta que quedó una acción pendiente, el bot adjunta los botones,
 * y el toque del usuario dispara la ejecución en CÓDIGO.
 *
 * userId y chatId llegan por ToolContext, nunca del modelo.
 */
@Component
@RequiredArgsConstructor
public class DebtWriteTools {

    private final FixedDebtService fixedDebtService;
    private final ConversationService conversationService;

    @Tool(description = "Prepara el pago de una deuda fija. Úsala cuando el usuario "
            + "diga que pagó o quiere pagar una deuda (ej. 'pagué 250 mil de la moto'). "
            + "Recibe el nombre/institución de la deuda y el monto. NO ejecuta el pago: "
            + "muestra la consecuencia; el usuario confirmará con un botón.")
    public String previewDebtPayment(
            String debtNameOrInstitution,
            BigDecimal amount,
            ToolContext toolContext) {

        String userId = (String) toolContext.getContext().get(ToolContextKeys.USER_ID);
        String chatId = (String) toolContext.getContext().get(ToolContextKeys.CHAT_ID);
        if (userId == null || chatId == null) {
            return "No se pudo identificar al usuario.";
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "El monto del pago debe ser mayor a cero.";
        }

        List<FixedDebtResponse> debts = fixedDebtService.getAllByUser(userId);
        FixedDebtResponse debt = debts.stream()
                .filter(d -> matches(d, debtNameOrInstitution))
                .findFirst()
                .orElse(null);

        if (debt == null) {
            return "No encontré una deuda que coincida con \"" + debtNameOrInstitution
                    + "\". El usuario tiene: "
                    + debts.stream().map(FixedDebtResponse::getInstitution).toList();
        }

        if (amount.compareTo(debt.getRemainingBalance()) > 0) {
            return ("El pago de %s supera el saldo restante de la deuda de %s, que es %s. "
                    + "Sugerí pagar el saldo completo en su lugar.")
                    .formatted(amount, debt.getInstitution(), debt.getRemainingBalance());
        }

        BigDecimal newBalance = debt.getRemainingBalance().subtract(amount);

        Conversation conv = conversationService.getOrCreate(chatId, userId);
        PendingAction action = PendingAction
                .of(PendingActionExecutor.PAY_DEBT,
                        "Pagar %s a la deuda de %s dejaría el saldo en %s. ¿Confirmás?"
                                .formatted(amount, debt.getInstitution(), newBalance))
                .with("debtId", debt.getId())
                .with("amount", amount.toPlainString());
        conversationService.setPending(conv, action);

        // El modelo comunica la consecuencia; los BOTONES (que agrega el bot)
        // se encargan de la confirmación. No pidas confirmación por texto.
        return ("PROPUESTA preparada. Comunicale al usuario esta consecuencia de forma "
                + "clara y breve: pagar %s a la deuda de %s dejaría el saldo en %s "
                + "(actual %s), próxima cuota el día %d. Aparecerán botones para "
                + "confirmar o cancelar; no le pidas que escriba nada.")
                .formatted(amount, debt.getInstitution(), newBalance,
                        debt.getRemainingBalance(), debt.getPaymentDueDay());
    }

    private boolean matches(FixedDebtResponse d, String query) {
        String q = query.toLowerCase();
        boolean inInstitution = d.getInstitution() != null
                && d.getInstitution().toLowerCase().contains(q);
        boolean inDescription = d.getDescription() != null
                && d.getDescription().toLowerCase().contains(q);
        return inInstitution || inDescription;
    }
}