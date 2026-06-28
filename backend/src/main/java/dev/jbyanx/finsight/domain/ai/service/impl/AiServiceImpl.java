package dev.jbyanx.finsight.domain.ai.service.impl;

import dev.jbyanx.finsight.domain.ai.context.FinancialContextBuilder;
import dev.jbyanx.finsight.domain.ai.context.SystemPrompts;
import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.ai.tool.CashTools;
import dev.jbyanx.finsight.domain.ai.tool.ToolContextKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orquesta el chat: arma el system prompt (reglas + contexto financiero del
 * usuario), registra las tools y pasa el userId por ToolContext.
 *
 * El system prompt se compone de dos partes:
 *   1. SystemPrompts.FINSIGHT_ADVISOR — genérico (personalidad + reglas de dominio).
 *   2. El contexto financiero del usuario — calculado por userId desde la BD.
 * Así, cada usuario recibe el mismo "cerebro" pero con SUS datos.
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final CashTools cashTools;
    private final FinancialContextBuilder contextBuilder;

    @Override
    public String chat(String userId, String message) {
        String systemPrompt = SystemPrompts.FINSIGHT_ADVISOR
                + "\n\n"
                + contextBuilder.buildFor(userId);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .tools(cashTools)
                .toolContext(Map.of(ToolContextKeys.USER_ID, userId))
                .call()
                .content();
    }
}