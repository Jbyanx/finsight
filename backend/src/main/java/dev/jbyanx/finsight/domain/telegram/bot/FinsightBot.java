package dev.jbyanx.finsight.domain.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Consumidor de updates de Telegram. Recibe mensajes y delega al
 * {@link MessageRouter}, que decide comando vs lenguaje natural.
 *
 * Sigue siendo "tonto": no conoce comandos ni IA, solo recibe, delega y responde.
 * El registro contra Telegram lo hace {@link TelegramBotRegistrar} de forma
 * tolerante a fallos.
 */
@Slf4j
public class FinsightBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final MessageRouter messageRouter;

    public FinsightBot(String botToken, MessageRouter messageRouter) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.messageRouter = messageRouter;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return; // por ahora ignoramos lo que no sea texto
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        String response;
        try {
            response = messageRouter.route(chatId, text);
        } catch (Exception e) {
            log.error("Error routing update from chat {}", chatId, e);
            response = "Ups, algo salió mal procesando tu mensaje. Intenta de nuevo.";
        }

        sendText(chatId, response);
    }

    private void sendText(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}", chatId, e);
        }
    }
}