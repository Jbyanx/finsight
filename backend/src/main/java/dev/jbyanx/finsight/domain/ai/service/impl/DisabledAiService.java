package dev.jbyanx.finsight.domain.ai.service.impl;

import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.telegram.bot.BotReply;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementación nula de {@link AiService} para cuando la IA está apagada
 * ({@code finsight.ai.enabled=false}, p. ej. en el perfil test).
 *
 * Con la IA desactivada no se autoconfigura el {@code ChatClient} (ver
 * {@link dev.jbyanx.finsight.domain.ai.AiConfig}), por lo que {@link AiServiceImpl}
 * tampoco se registra. Este fallback mantiene un bean de {@link AiService}
 * disponible para que sus consumidores (p. ej. el MessageRouter) sigan
 * cableando y el ApplicationContext cargue.
 */
@Service
@ConditionalOnProperty(name = "finsight.ai.enabled", havingValue = "false")
public class DisabledAiService implements AiService {

    private static final BotReply DISABLED =
            BotReply.text("El asistente de IA está desactivado en este entorno.");

    @Override
    public BotReply chat(String chatId, String userId, String message) {
        return DISABLED;
    }

    @Override
    public BotReply handleConfirmation(String chatId, String userId, String callbackData) {
        return DISABLED;
    }
}
