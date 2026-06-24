package dev.jbyanx.finsight.domain.ai.service;

/**
 * Servicio mínimo de IA. Por ahora solo manda un prompt y devuelve el texto,
 * para verificar que la conexión con Gemini funciona (smoke test del issue #50).
 *
 * En issues siguientes este servicio crecerá: contexto financiero, tools, memoria.
 */
public interface AiService {

    /**
     * Manda un prompt simple al modelo y devuelve la respuesta en texto plano.
     *
     * @param prompt el mensaje del usuario
     * @return la respuesta del modelo
     */
    String ask(String prompt);
}