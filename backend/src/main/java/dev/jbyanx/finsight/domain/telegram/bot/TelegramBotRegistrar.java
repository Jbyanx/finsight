package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandDispatcher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

/**
 * Registra el bot contra Telegram de forma MANUAL y TOLERANTE A FALLOS.
 *
 * Por qué no usamos el autoregistro del starter (SpringLongPollingBot):
 * ese camino deja que el `telegramBotInitializer` registre el bot durante el
 * arranque del contexto, y si Telegram responde error (404 por token inválido,
 * red caída, etc.) la excepción PROPAGA y tumba todo el ApplicationContext.
 * El bot es un cliente opcional: no puede impedir que arranque la API/PWA.
 *
 * Aquí registramos en ApplicationReadyEvent (contexto ya levantado) y envolvemos
 * en try/catch: si falla, log de warning y la app sigue viva, sin bot.
 *
 * Se activa solo si telegram.bot.enabled=true (en test está en false).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = false)
public class TelegramBotRegistrar {

    private final String botToken;
    private final CommandDispatcher dispatcher;
    private TelegramBotsLongPollingApplication botsApplication;

    public TelegramBotRegistrar(@Value("${telegram.bot.token:}") String botToken,
                                CommandDispatcher dispatcher) {
        this.botToken = botToken;
        this.dispatcher = dispatcher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerBot() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram bot enabled but no token provided — bot will NOT start.");
            return;
        }
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new FinsightBot(botToken, dispatcher));
            log.info("Telegram bot registered and running.");
        } catch (Exception e) {
            // No relanzamos: el backend debe seguir vivo aunque el bot falle.
            log.warn("Telegram bot failed to register ({}). "
                    + "The backend will run without the bot.", e.getClass().getSimpleName());
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