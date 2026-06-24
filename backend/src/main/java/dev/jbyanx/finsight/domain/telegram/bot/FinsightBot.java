package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Consumidor de updates de Telegram. Recibe mensajes y delega al
 * {@link CommandDispatcher}.
 *
 * IMPORTANTE: esta clase ya NO implementa SpringLongPollingBot ni es un @Component.
 * El registro contra Telegram lo hace {@link TelegramBotRegistrar} de forma manual
 * y tolerante a fallos, para que un token inválido o Telegram caído NO tumbe el
 * arranque del backend (el bot es un cliente opcional del sistema).
 *
 * La instancia la crea TelegramBotRegistrar, no Spring directamente.
 */
@Slf4j
public class FinsightBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final CommandDispatcher dispatcher;

    public FinsightBot(String botToken, CommandDispatcher dispatcher) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.dispatcher = dispatcher;
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