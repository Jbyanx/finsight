package dev.jbyanx.finsight.domain.ai.service.impl;

import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.ai.tool.CashTools;
import dev.jbyanx.finsight.domain.ai.tool.ToolContextKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orquesta el chat con el modelo: registra las tools y le pasa el userId
 * por el ToolContext.
 *
 * Patrón fluido:
 *   prompt().user(msg).tools(...).toolContext(...).call().content()
 *
 * - tools(...): expone las tools al modelo (Spring AI genera su esquema).
 * - toolContext(...): datos del servidor para las tools (el userId), invisibles
 *   para el modelo.
 * - call().content(): ejecuta el ciclo completo (incluida la llamada a tools si
 *   el modelo las pide) y devuelve el texto final.
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final CashTools cashTools;

    @Override
    public String chat(String userId, String message) {
        return chatClient.prompt()
                .user(message)
                .tools(cashTools)
                .toolContext(Map.of(ToolContextKeys.USER_ID, userId))
                .call()
                .content();
    }
}