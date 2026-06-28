package dev.jbyanx.finsight.domain.telegram.bot;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

/**
 * Registra el bot contra Telegram de forma MANUAL y TOLERANTE A FALLOS,
 * para que un token inválido o Telegram caído NO tumben el arranque del backend.
 *
 * Se activa solo si telegram.bot.enabled=true (en test está en false).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = false)
public class TelegramBotRegistrar {

    private final String botToken;
    private final MessageRouter messageRouter;
    private TelegramBotsLongPollingApplication botsApplication;

    public TelegramBotRegistrar(@Value("${telegram.bot.token:}") String botToken,
                                MessageRouter messageRouter) {
        this.botToken = botToken;
        this.messageRouter = messageRouter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerBot() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram bot enabled but no token provided — bot will NOT start.");
            return;
        }
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new FinsightBot(botToken, messageRouter));
            log.info("Telegram bot registered and running.");
        } catch (Exception e) {
            log.warn("Telegram bot failed to register ({}). "
                    + "The backend will run without the bot.", e.getMessage());
            closeQuietly();
        }
    }

    @PreDestroy
    public void shutdown() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (botsApplication != null) {
            try {
                botsApplication.close();
            } catch (Exception e) {
                log.warn("Error closing Telegram bot application: {}", e.getMessage());
            } finally {
                botsApplication = null;
            }
        }
    }
}