package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.telegram.bot.command.CommandDispatcher;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Decide comando vs lenguaje natural (para texto) y maneja callbacks de botones.
 * El userId SIEMPRE se resuelve desde el chatId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private final CommandDispatcher commandDispatcher;
    private final TelegramLinkService telegramLinkService;
    private final AiService aiService;

    public BotReply route(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return BotReply.text("No te entendí. Escribí /help para ver qué puedo hacer.");
        }

        String trimmed = text.strip();

        if (trimmed.startsWith("/")) {
            return BotReply.text(commandDispatcher.dispatch(chatId, trimmed));
        }

        String userId = telegramLinkService.resolveUserId(chatId);
        if (userId == null) {
            return BotReply.text(notLinked());
        }

        try {
            return aiService.chat(String.valueOf(chatId), userId, trimmed);
        } catch (Exception e) {
            log.error("AI chat failed for chat {}", chatId, e);
            return BotReply.text("Estoy teniendo problemas para procesar eso ahora mismo. "
                    + "Intentá de nuevo en un momento.");
        }
    }

    public BotReply routeCallback(long chatId, String callbackData) {
        String userId = telegramLinkService.resolveUserId(chatId);
        if (userId == null) {
            return BotReply.text("Tu sesión no está vinculada. Enviá /start TU_CODIGO.");
        }
        try {
            return aiService.handleConfirmation(String.valueOf(chatId), userId, callbackData);
        } catch (Exception e) {
            log.error("Callback handling failed for chat {}", chatId, e);
            return BotReply.text("No pude completar la acción. Intentá de nuevo.");
        }
    }

    private String notLinked() {
        return """
                Para conversar conmigo primero vinculá tu cuenta. 🔗

                Abrí Finsight, generá tu código y enviame:
                /start TU_CODIGO""";
    }
}