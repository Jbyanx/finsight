package dev.jbyanx.finsight.domain.ai.tool;

/**
 * Claves usadas en el ToolContext de Spring AI.
 *
 * El ToolContext es un canal del lado del servidor para pasar datos a las tools
 * que el modelo NO ve ni controla. Aquí vive el userId: nunca viene del modelo,
 * siempre se resuelve desde el chatId y se inyecta por este canal.
 */
public final class ToolContextKeys {

    private ToolContextKeys() {
    }

    /** userId del usuario autenticado, resuelto desde el chatId de Telegram. */
    public static final String USER_ID = "userId";
}