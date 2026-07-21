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
 * Consumidor de updates de Telegram (long polling, hilo único).
 *
 * El formato de salida se delega a {@link TelegramFormatter} (HTML). Se mantiene
 * el fallback a texto plano como red de seguridad ante cualquier fallo de envío.
 */
@Slf4j
public class FinsightBot implements LongPollingSingleThreadUpdateConsumer {

    private static final String PARSE_MODE = "HTML";

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
            reply = BotReply.text("Algo salió mal procesando tu mensaje. Intenta de nuevo.");
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
            reply = BotReply.text("Algo salió mal. Intenta de nuevo.");
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
        String formatted = TelegramFormatter.toHtml(reply.text());

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
            // Red de seguridad: el usuario recibe la respuesta igual, sin formato.
            log.warn("HTML send failed for chat {} ({}). Retrying as plain text.",
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
}