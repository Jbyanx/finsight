package dev.jbyanx.finsight.domain.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Spring AI.
 *
 * El starter de Google GenAI autoconfigura un {@link org.springframework.ai.chat.model.ChatModel}
 * a partir de la API key en properties. Aquí construimos UN {@link ChatClient} encima de él,
 * que es la API cómoda que vamos a inyectar en los services.
 *
 * Por ahora el ChatClient no lleva system prompt ni tools — eso llega en los issues
 * #51 (tools) y #52 (contexto financiero). Este issue solo deja el cableado base listo.
 */
@Configuration
public class AiConfig {

    /**
     * ChatClient base. Spring inyecta automáticamente el ChatClient.Builder
     * que el starter ya configuró con el ChatModel de Gemini.
     */
    @Bean
    @ConditionalOnProperty(name = "finsight.ai.enabled", havingValue = "true", matchIfMissing = true)
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}