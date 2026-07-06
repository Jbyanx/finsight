package dev.jbyanx.finsight.domain.telegram.bot;

/**
 * Respuesta del router hacia el bot: el texto y si debe llevar botones de
 * confirmación (✅/❌). Permite que el bot decida cómo enviar sin conocer la
 * lógica de negocio.
 */
public record BotReply(String text, boolean withConfirmButtons) {

    public static BotReply text(String text) {
        return new BotReply(text, false);
    }

    public static BotReply withButtons(String text) {
        return new BotReply(text, true);
    }
}