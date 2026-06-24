package dev.jbyanx.finsight.domain.telegram.bot.command;

/**
 * Contrato de un comando del bot. Cada comando (/start, /help, ...) implementa esto.
 * El dispatcher selecciona el handler cuyo {@link #command()} coincide con el texto.
 */
public interface CommandHandler {

    /** El comando que maneja, incluyendo la barra. Ej: "/start". */
    String command();

    /**
     * Procesa el mensaje y devuelve el texto de respuesta para el usuario.
     *
     * @param chatId  id del chat de Telegram desde donde llegó el mensaje
     * @param args    el resto del mensaje tras el comando (puede ser vacío)
     * @return texto a enviar de vuelta
     */
    String handle(long chatId, String args);
}