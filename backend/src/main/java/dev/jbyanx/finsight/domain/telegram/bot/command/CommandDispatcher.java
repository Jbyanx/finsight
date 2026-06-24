package dev.jbyanx.finsight.domain.telegram.bot.command;

import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enruta el texto entrante al {@link CommandHandler} correspondiente y aplica
 * el bloqueo de chats no vinculados.
 *
 * Regla de vinculación: si el comando {@link CommandHandler#requiresLink()} es
 * true y el chat no está vinculado (resolveUserId == null), no se enruta y se
 * devuelve un mensaje guía. /start y /help declaran requiresLink()=false, así
 * que siempre pasan.
 *
 * Agregar un comando nuevo sigue siendo solo crear otro handler: este dispatcher
 * no se toca.
 */
@Slf4j
@Component
public class CommandDispatcher {

    private final Map<String, CommandHandler> handlers;
    private final TelegramLinkService telegramLinkService;

    public CommandDispatcher(List<CommandHandler> handlerList,
                             TelegramLinkService telegramLinkService) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        h -> h.command().toLowerCase(),
                        Function.identity()));
        this.telegramLinkService = telegramLinkService;
        log.info("Registered {} command handler(s): {}",
                handlers.size(), handlers.keySet());
    }

    /**
     * Resuelve y ejecuta el comando contenido en el texto.
     *
     * @param chatId id del chat de Telegram
     * @param text   texto crudo del mensaje (ej: "/start ABC234")
     * @return respuesta a enviar
     */
    public String dispatch(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return unknownCommand();
        }

        String trimmed = text.strip();
        int spaceIdx = trimmed.indexOf(' ');
        String command = (spaceIdx == -1 ? trimmed : trimmed.substring(0, spaceIdx))
                .toLowerCase();
        String args = spaceIdx == -1 ? "" : trimmed.substring(spaceIdx + 1).strip();

        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            return unknownCommand();
        }

        // Bloqueo de vinculación: comandos protegidos exigen chat vinculado.
        if (handler.requiresLink() && telegramLinkService.resolveUserId(chatId) == null) {
            return notLinked();
        }

        return handler.handle(chatId, args);
    }

    private String unknownCommand() {
        return "No reconozco ese comando. Escribe /help para ver lo que puedo hacer.";
    }

    private String notLinked() {
        return """
                Todavía no vinculaste tu cuenta. 🔗

                Abre Finsight, genera tu código de vinculación y envíame:
                /start TU_CODIGO""";
    }
}