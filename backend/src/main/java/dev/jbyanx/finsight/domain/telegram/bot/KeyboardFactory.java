package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.ai.pending.CallbackActions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

/**
 * Construye teclados inline. Por ahora, el de confirmación (Confirmar / Cancelar).
 */
public final class KeyboardFactory {

    private KeyboardFactory() {
    }

    public static InlineKeyboardMarkup confirmCancel() {
        InlineKeyboardButton confirm = InlineKeyboardButton.builder()
                .text("✅ Confirmar")
                .callbackData(CallbackActions.CONFIRM)
                .build();
        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
                .text("❌ Cancelar")
                .callbackData(CallbackActions.CANCEL)
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(confirm, cancel)));
    }
}