package dev.jbyanx.finsight.domain.ai.service.impl;

import dev.jbyanx.finsight.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Implementación mínima sobre {@link ChatClient}.
 *
 * El patrón fluido de ChatClient es: prompt() -> user(...) -> call() -> content().
 * - prompt(): inicia la construcción de la petición.
 * - user(...): el mensaje del usuario.
 * - call(): ejecuta la llamada al modelo (bloqueante).
 * - content(): extrae solo el texto de la respuesta.
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    @Override
    public String ask(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}