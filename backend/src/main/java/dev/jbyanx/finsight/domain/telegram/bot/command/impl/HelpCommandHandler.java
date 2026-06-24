package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * Maneja /help. Lista lo que el bot puede hacer.
 * Se irá ampliando a medida que se agreguen comandos en el milestone.
 */
@Component
public class HelpCommandHandler implements CommandHandler {

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String handle(long chatId, String args) {
        return """
                Esto es lo que puedo hacer por ahora:

                /start — iniciar y (pronto) vincular tu cuenta
                /help — mostrar esta ayuda

                Muy pronto vas a poder registrar gastos, consultar tus tarjetas
                y pedir recomendaciones, todo en lenguaje natural.""";
    }
}