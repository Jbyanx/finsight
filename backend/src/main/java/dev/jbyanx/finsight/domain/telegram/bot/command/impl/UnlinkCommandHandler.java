package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandHandler;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maneja /unlink — desvincula la cuenta desde el propio bot.
 *
 * POR QUÉ IMPORTA:
 * Sin esto, la única forma de desvincular era desde la web. Si alguien pierde el
 * acceso a su cuenta web, o presta el teléfono, no tenía manera de cortar el
 * acceso del chat a sus datos financieros.
 *
 * Al desvincular se borra el TelegramLink (Postgres) Y la Conversation (Mongo),
 * con su historial y cualquier acción pendiente sin confirmar.
 *
 * requiresLink = true: no tiene sentido desvincular lo que no está vinculado; el
 * dispatcher ya responde el mensaje de "vincula primero".
 */
@Component
@RequiredArgsConstructor
public class UnlinkCommandHandler implements CommandHandler {

    private final TelegramLinkService telegramLinkService;

    @Override
    public String command() {
        return "/unlink";
    }

    @Override
    public boolean requiresLink() {
        return true;
    }

    @Override
    public String handle(long chatId, String args) {
        boolean removed = telegramLinkService.unlinkByChatId(chatId);

        if (!removed) {
            return "Esta cuenta no estaba vinculada.";
        }

        return """
                Listo, desvinculé tu cuenta.

                Borré el historial de esta conversación y cualquier acción
                pendiente. Tus datos financieros siguen intactos en Finsight.

                Para volver a conectarte, genera un código nuevo y envía:
                /start TU_CODIGO""";
    }
}