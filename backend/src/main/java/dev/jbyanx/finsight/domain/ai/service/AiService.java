package dev.jbyanx.finsight.domain.ai.service;

import dev.jbyanx.finsight.domain.telegram.bot.BotReply;

/**
 * Servicio de IA con memoria y confirmación por botones.
 */
public interface AiService {

    /**
     * Procesa un mensaje de texto del usuario. Si el modelo prepara una mutación
     * (deja una acción pendiente), la respuesta vuelve con withConfirmButtons=true
     * para que el bot muestre los botones Confirmar/Cancelar.
     */
    BotReply chat(String chatId, String userId, String message);

    /**
     * Procesa el toque de un botón de confirmación (confirm/cancel). Ejecuta o
     * cancela la acción pendiente (en CÓDIGO) y devuelve el resultado.
     */
    BotReply handleConfirmation(String chatId, String userId, String callbackData);
}