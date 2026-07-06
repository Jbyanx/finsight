package dev.jbyanx.finsight.domain.ai.service.impl;

import dev.jbyanx.finsight.domain.ai.context.FinancialContextBuilder;
import dev.jbyanx.finsight.domain.ai.context.SystemPrompts;
import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import dev.jbyanx.finsight.domain.ai.pending.ConfirmationHandler;
import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.ai.tool.*;
import dev.jbyanx.finsight.domain.telegram.bot.BotReply;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orquesta el chat con memoria y confirmación por botones.
 *
 * IMPORTANTE (fix race condition): la tool de escritura (previewDebtPayment)
 * guarda la PendingAction en SU propia carga de la conversación durante la
 * llamada al modelo. Por eso, para NO pisar ese pending, después de la llamada
 * recargamos la conversación y registramos el historial sobre ESA instancia
 * (la que ya tiene el pending), no sobre la que cargamos al inicio.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "finsight.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final FinancialContextBuilder contextBuilder;
    private final ConversationService conversationService;
    private final ConfirmationHandler confirmationHandler;

    private final CashTools cashTools;
    private final CardTools cardTools;
    private final DebtTools debtTools;
    private final SummaryTools summaryTools;
    private final DebtWriteTools debtWriteTools;

    @Override
    public BotReply chat(String chatId, String userId, String message) {
        // Cargamos SOLO para armar el historial del prompt.
        Conversation before = conversationService.getOrCreate(chatId, userId);

        String systemPrompt = SystemPrompts.FINSIGHT_ADVISOR
                + "\n\n" + contextBuilder.buildFor(userId)
                + "\n\n" + recentHistory(before);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .tools(cashTools, cardTools, debtTools, summaryTools, debtWriteTools)
                .toolContext(Map.of(
                        ToolContextKeys.USER_ID, userId,
                        ToolContextKeys.CHAT_ID, chatId))
                .call()
                .content();

        // RECARGAMOS: si una tool guardó un pending durante la llamada, esta
        // instancia lo tiene. Registramos el historial sobre ESTA, sin pisar
        // el pending.
        Conversation after = conversationService.getOrCreate(chatId, userId);
        boolean needsConfirm = after.getPendingAction() != null;

        if (response == null || response.isBlank()) {
            if (needsConfirm && after.getPendingAction().getPreviewText() != null) {
                response = after.getPendingAction().getPreviewText();
            } else {
                response = "Listo. ¿Algo más en lo que te pueda ayudar?";
            }
        }

        // recordExchange guarda 'after' (con su pending intacto) + el historial.
        conversationService.recordExchange(after, message, response);

        return needsConfirm ? BotReply.withButtons(response) : BotReply.text(response);
    }

    @Override
    public BotReply handleConfirmation(String chatId, String userId, String callbackData) {
        String result = confirmationHandler.handleCallback(chatId, userId, callbackData);
        if (result == null || result.isBlank()) {
            result = "Listo.";
        }
        return BotReply.text(result);
    }

    private String recentHistory(Conversation conv) {
        if (conv.getMessages().isEmpty()) {
            return "HISTORIAL RECIENTE: (sin mensajes previos)";
        }
        String history = conv.getMessages().stream()
                .map(m -> (m.getRole().equals("user") ? "Usuario" : "Finsight") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
        return "HISTORIAL RECIENTE DE LA CONVERSACIÓN:\n" + history;
    }
}