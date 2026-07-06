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
                *Soy Finsight, tu asistente financiero.* 🪙

                Háblame en lenguaje natural, como a un asesor. Por ejemplo:

                *Consultar:*
                • "¿cuánto tengo en efectivo?"
                • "¿cuáles son mis tarjetas?"
                • "¿con qué tarjeta pago hoy?"
                • "¿cuánto debo?"
                • "¿cómo estoy financieramente?"

                *Registrar movimientos:*
                • "pagué 250 mil de la moto"
                (te muestro la consecuencia y confirmás con un botón)

                *Comandos:*
                /start — vincular tu cuenta (/start TU_CODIGO)
                /help — mostrar esta ayuda

                Te doy recomendaciones para cuidar tu historial crediticio,
                tu utilización y tus metas de ahorro. ¡Pregúntame lo que sea!""";
    }
}