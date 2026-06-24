package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * Maneja /start.
 *
 * Por ahora solo saluda. En el issue #49 este handler tomará el código de
 * vinculación (/start <code>) y llamará a TelegramLinkService.linkAccount(...).
 */
@Component
public class StartCommandHandler implements CommandHandler {

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String handle(long chatId, String args) {
        return """
                ¡Hola! Soy Finsight, tu asistente financiero. 🪙

                Todavía no estoy vinculado a tu cuenta. La vinculación llega muy pronto.
                Por ahora, escribe /help para ver lo que puedo hacer.""";
    }
}