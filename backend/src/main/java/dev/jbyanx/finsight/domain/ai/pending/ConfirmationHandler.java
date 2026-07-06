package dev.jbyanx.finsight.domain.ai.pending;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resuelve una confirmación disparada por un BOTÓN inline (callback).
 *
 * Ya no interpreta texto: el usuario tocó Confirmar o Cancelar, así que recibimos
 * un valor determinista (CallbackActions.CONFIRM / CANCEL). El CÓDIGO ejecuta o
 * descarta; el modelo no participa. Cero ambigüedad.
 */
@Component
@RequiredArgsConstructor
public class ConfirmationHandler {

    private final ConversationService conversationService;
    private final PendingActionExecutor executor;

    /**
     * Procesa el toque de un botón de confirmación.
     *
     * @param chatId chat de Telegram
     * @param userId usuario dueño del chat
     * @param callbackData valor del botón (confirm/cancel)
     * @return mensaje a mostrar al usuario tras resolver
     */
    public String handleCallback(String chatId, String userId, String callbackData) {
        Conversation conv = conversationService.getOrCreate(chatId, userId);
        PendingAction pending = conv.getPendingAction();

        if (pending == null) {
            return "Esa acción ya no está disponible. ¿En qué más te ayudo?";
        }

        String result;
        if (CallbackActions.CONFIRM.equals(callbackData)) {
            result = executor.execute(userId, pending);
        } else {
            result = "Listo, cancelado. No hice ningún cambio.";
        }

        conversationService.clearPending(conv);
        conversationService.recordExchange(conv,
                "[botón: " + callbackData + "]", result);
        return result;
    }
}