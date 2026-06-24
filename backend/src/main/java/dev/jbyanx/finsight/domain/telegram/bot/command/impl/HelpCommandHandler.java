package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * Maneja /help. Disponible sin vinculación (requiresLink=false) para que
 * un usuario nuevo pueda pedir ayuda antes de vincular.
 */
@Component
public class HelpCommandHandler implements CommandHandler {

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public boolean requiresLink() {
        return false;
    }

    @Override
    public String handle(long chatId, String args) {
        return """
                Esto es lo que puedo hacer por ahora:

                /start — iniciar y vincular tu cuenta (/start TU_CODIGO)
                /help — mostrar esta ayuda

                Muy pronto vas a poder registrar gastos, consultar tus tarjetas
                y pedir recomendaciones, todo en lenguaje natural.""";
    }
}