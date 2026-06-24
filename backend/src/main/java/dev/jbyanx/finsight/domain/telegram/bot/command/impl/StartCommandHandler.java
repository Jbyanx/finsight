package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.bot.command.CommandHandler;
import dev.jbyanx.finsight.domain.telegram.dto.TelegramLinkResponse;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maneja /start.
 *
 * - "/start <code>" -> intenta vincular la cuenta con ese código.
 * - "/start" sin código -> mensaje de bienvenida con instrucciones.
 *
 * No requiere vinculación previa (requiresLink=false): es justamente el comando
 * que vincula, así que debe funcionar para chats no vinculados.
 *
 * El userId NUNCA viene del mensaje: linkAccount lo deriva del código (que el
 * usuario generó ya autenticado en la app). Aquí solo pasamos code + chatId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final TelegramLinkService telegramLinkService;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public boolean requiresLink() {
        return false;
    }

    @Override
    public String handle(long chatId, String args) {
        if (args == null || args.isBlank()) {
            return welcome();
        }

        String code = args.strip();
        try {
            TelegramLinkResponse result = telegramLinkService.linkAccount(code, chatId);
            return "¡Listo, %s! 🎉 Tu cuenta quedó vinculada.%n%nEscribe /help para empezar."
                    .formatted(result.getUserName());
        } catch (UnauthorizedException e) {
            // Mensaje de la excepción viene en inglés; lo traducimos a algo amable.
            log.info("Link attempt failed for chat {}: {}", chatId, e.getMessage());
            return linkError();
        }
    }

    private String welcome() {
        return """
                ¡Hola! Soy Finsight, tu asistente financiero. 🪙

                Para vincular tu cuenta, genera un código en la app y envíame:
                /start TU_CODIGO

                Escribe /help si necesitas ayuda.""";
    }

    private String linkError() {
        return """
                No pude vincular tu cuenta. 😕

                El código es inválido, ya expiró, o este Telegram ya está
                vinculado a otra cuenta. Genera uno nuevo en la app e inténtalo
                de nuevo: /start TU_CODIGO""";
    }
}