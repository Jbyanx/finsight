package dev.jbyanx.finsight.domain.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Consumidor de updates. Maneja mensajes de texto y callbacks de botones,
 * delega en MessageRouter y responde. Cumple el protocolo de Telegram
 * (answerCallbackQuery obligatorio tras un toque de botón).
 *
 * FORMATO: los mensajes se envían con parse mode "Markdown" (legacy), que es
 * permisivo con puntos, signos $, etc. El modelo escribe negritas con doble
 * asterisco (**texto**, estilo estándar), pero el Markdown legacy de Telegram
 * usa asterisco simple (*texto*), así que convertimos ** -> * antes de enviar.
 *
 * SEGURIDAD DE ENVÍO: si Telegram rechaza el mensaje por un problema de formato
 * (algún carácter que rompe el Markdown), reintentamos SIN parse mode (texto
 * plano). Así el usuario SIEMPRE recibe la respuesta, aunque pierda el formato.
 */
@Slf4j
public class FinsightBot implements LongPollingSingleThreadUpdateConsumer {

    private static final String PARSE_MODE = "Markdown";

    private final TelegramClient telegramClient;
    private final MessageRouter messageRouter;

    public FinsightBot(String botToken, MessageRouter messageRouter) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.messageRouter = messageRouter;
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        }
    }

    private void handleTextMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        BotReply reply;
        try {
            reply = messageRouter.route(chatId, text);
        } catch (Exception e) {
            log.error("Error routing message from chat {}", chatId, e);
            reply = BotReply.text("Ups, algo salió mal procesando tu mensaje. Intentá de nuevo.");
        }
        send(chatId, reply);
    }

    private void handleCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        String data = callback.getData();

        answerCallback(callback.getId()); // OBLIGATORIO: quita la barra de "cargando"

        BotReply reply;
        try {
            reply = messageRouter.routeCallback(chatId, data);
        } catch (Exception e) {
            log.error("Error routing callback from chat {}", chatId, e);
            reply = BotReply.text("Ups, algo salió mal. Intentá de nuevo.");
        }
        send(chatId, reply);
    }

    private void answerCallback(String callbackId) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback {}", callbackId, e);
        }
    }

    private void send(long chatId, BotReply reply) {
        String formatted = toTelegramMarkdown(reply.text());

        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(chatId)
                .text(formatted)
                .parseMode(PARSE_MODE);

        if (reply.withConfirmButtons()) {
            builder.replyMarkup(KeyboardFactory.confirmCancel());
        }

        try {
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            // El Markdown pudo romper por algún carácter. Reintentamos en texto
            // plano para que el usuario reciba la respuesta igual.
            log.warn("Markdown send failed for chat {} ({}). Retrying as plain text.",
                    chatId, e.getMessage());
            sendPlain(chatId, reply);
        }
    }

    private void sendPlain(long chatId, BotReply reply) {
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(chatId)
                .text(reply.text()); // texto original, sin conversión ni parse mode

        if (reply.withConfirmButtons()) {
            builder.replyMarkup(KeyboardFactory.confirmCancel());
        }

        try {
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("Plain-text send also failed for chat {}", chatId, e);
        }
    }

    /**
     * Adapta el texto del modelo al Markdown legacy de Telegram:
     *  - Encabezados (#, ##, ###) -> negrita (Telegram no soporta headers).
     *  - Negrita estándar (**texto**) -> asterisco simple (*texto*).
     */
    private String toTelegramMarkdown(String text) {
        if (text == null) return "";

        String result = text;

        // 1) Headers Markdown "### Título" -> "*Título*" (línea por línea).
        //    Quita los # del inicio de línea y pone el título en negrita.
        result = result.replaceAll("(?m)^\\s*#{1,6}\\s*(.+?)\\s*$", "*$1*");

        // 2) Negrita doble -> simple (Telegram legacy usa un asterisco).
        result = result.replaceAll("\\*{2,}", "*");

        return result;
    }
    /*
    Notas del regex de headers:
  (?m)      -> modo multilínea, ^ y $ aplican por línea
  ^\s*#{1,6}\s*  -> inicio de línea, espacios, 1 a 6 #, espacios
  (.+?)     -> el texto del título (capturado)
  \s*$      -> espacios finales hasta fin de línea
  "*$1*"    -> el título envuelto en asteriscos (negrita Telegram)
    * */
}