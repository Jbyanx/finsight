package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Bot de Telegram en modo long-polling (telegrambots 10.x).
 *
 * Responsabilidad única: recibir updates y delegar al {@link CommandDispatcher}.
 * No conoce comandos concretos — eso vive en los CommandHandler.
 *
 * El starter registra este bean automáticamente al arrancar; no hace falta
 * registro manual.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.bot.token", matchIfMissing = false)
public class FinsightBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final TelegramClient telegramClient;
    private final CommandDispatcher dispatcher;

    public FinsightBot(@Value("${telegram.bot.token}") String botToken,
                       CommandDispatcher dispatcher) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.dispatcher = dispatcher;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return; // ignoramos por ahora lo que no sea texto
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        String response;
        try {
            response = dispatcher.dispatch(chatId, text);
        } catch (Exception e) {
            log.error("Error handling update from chat {}", chatId, e);
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