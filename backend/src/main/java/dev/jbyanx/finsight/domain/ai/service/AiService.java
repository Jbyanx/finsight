package dev.jbyanx.finsight.domain.ai.service;

/**
 * Servicio de IA. Manda el mensaje del usuario al modelo con las tools
 * disponibles, y devuelve la respuesta en lenguaje natural.
 */
public interface AiService {

    /**
     * Procesa un mensaje en lenguaje natural para un usuario concreto.
     *
     * El userId se inyecta en el ToolContext (canal del servidor), de modo que
     * las tools puedan resolver "de quién" son los datos sin que el modelo lo
     * controle. NUNCA viene del mensaje.
     *
     * @param userId id del usuario autenticado (resuelto desde el chatId)
     * @param message texto en lenguaje natural del usuario
     * @return respuesta del modelo
     */
    String chat(String userId, String message);
}