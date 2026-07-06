package dev.jbyanx.finsight.domain.ai.pending;

/**
 * Valores de callbackData de los botones inline de confirmación.
 *
 * Son deterministas y cortos (límite de 64 bytes de Telegram). Como el usuario
 * TOCA el botón en vez de escribir, no hay ambigüedad: el bot recibe exactamente
 * uno de estos valores.
 */
public final class CallbackActions {

    private CallbackActions() {
    }

    public static final String CONFIRM = "confirm";
    public static final String CANCEL = "cancel";
}