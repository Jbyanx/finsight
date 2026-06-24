package dev.jbyanx.finsight.domain.telegram.bot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enruta el texto entrante al {@link CommandHandler} correspondiente.
 *
 * Spring inyecta automáticamente todos los beans CommandHandler de la aplicación,
 * así que agregar un comando nuevo (linking en #49, etc.) es solo crear otro
 * handler — este dispatcher no se toca.
 */
@Slf4j
@Component
public class CommandDispatcher {

    private final Map<String, CommandHandler> handlers;

    public CommandDispatcher(List<CommandHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        h -> h.command().toLowerCase(),
                        Function.identity()));
        log.info("Registered {} command handler(s): {}",
                handlers.size(), handlers.keySet());
    }

    /**
     * Resuelve y ejecuta el comando contenido en el texto.
     *
     * @param chatId id del chat de Telegram
     * @param text   texto crudo del mensaje (ej: "/start ABC234")
     * @return respuesta a enviar; mensaje de ayuda si el comando es desconocido
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
        return handler.handle(chatId, args);
    }

    private String unknownCommand() {
        return "No reconozco ese comando. Escribe /help para ver lo que puedo hacer.";
    }
}