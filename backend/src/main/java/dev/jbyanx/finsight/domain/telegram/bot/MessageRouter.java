package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.telegram.bot.command.CommandDispatcher;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Decide qué hacer con cada mensaje de texto entrante:
 *
 *   - Empieza con "/"  -> es un comando, va al CommandDispatcher (como antes).
 *   - No empieza con "/" -> es lenguaje natural -> al AiService (Jarvis).
 *
 * Para el lenguaje natural aplica el bloqueo de vinculación: si el chat no está
 * vinculado, no llama a la IA (no tendríamos userId con quién consultar) y pide
 * vincular. El userId SIEMPRE se resuelve desde el chatId, nunca del mensaje.
 *
 * Mantiene al FinsightBot tonto: el bot solo delega aquí.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private final CommandDispatcher commandDispatcher;
    private final TelegramLinkService telegramLinkService;
    private final AiService aiService;

    public String route(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return "No te entendí. Escribe /help para ver qué puedo hacer.";
        }

        String trimmed = text.strip();

        // Comandos -> dispatcher (este ya maneja su propio gate de vinculación)
        if (trimmed.startsWith("/")) {
            return commandDispatcher.dispatch(chatId, trimmed);
        }

        // Lenguaje natural -> IA, pero solo si el chat está vinculado
        String userId = telegramLinkService.resolveUserId(chatId);
        if (userId == null) {
            return notLinked();
        }

        try {
            return aiService.chat(userId, trimmed);
        } catch (Exception e) {
            log.error("AI chat failed for chat {}", chatId, e);
            return "Estoy teniendo problemas para procesar eso ahora mismo. "
                    + "Intenta de nuevo en un momento.";
        }
    }

    private String notLinked() {
        return """
                Para conversar conmigo primero vincula tu cuenta. 🔗

                Abre Finsight, generá tu código y enviame:
                /start TU_CODIGO""";
    }
}