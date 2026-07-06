package dev.jbyanx.finsight.domain.ai.tool;

/**
 * Claves usadas en el ToolContext de Spring AI.
 *
 * Canal del lado del servidor para pasar datos a las tools que el modelo NO ve
 * ni controla. Aquí viven el userId y el chatId: nunca vienen del modelo.
 */
public final class ToolContextKeys {

    private ToolContextKeys() {
    }

    /** userId del usuario autenticado, resuelto desde el chatId de Telegram. */
    public static final String USER_ID = "userId";

    /** chatId de Telegram, para ubicar la conversación (memoria / acción pendiente). */
    public static final String CHAT_ID = "chatId";
}