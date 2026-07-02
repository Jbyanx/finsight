package dev.jbyanx.finsight.domain.ai.service.impl;

import dev.jbyanx.finsight.domain.ai.context.FinancialContextBuilder;
import dev.jbyanx.finsight.domain.ai.context.SystemPrompts;
import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.ai.tool.CardTools;
import dev.jbyanx.finsight.domain.ai.tool.CashTools;
import dev.jbyanx.finsight.domain.ai.tool.DebtTools;
import dev.jbyanx.finsight.domain.ai.tool.SummaryTools;
import dev.jbyanx.finsight.domain.ai.tool.ToolContextKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orquesta el chat: system prompt (reglas + contexto del usuario), tools y userId.
 *
 * Tools registradas (todas de LECTURA por ahora):
 *   - CashTools     : saldo de efectivo
 *   - CardTools     : tarjetas y utilización
 *   - DebtTools     : deudas fijas
 *   - SummaryTools  : resumen financiero completo
 *
 * Las de ESCRITURA (registrar gasto, pagar deuda, etc.) llegan después del #54
 * (confirmación antes de mutar).
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final FinancialContextBuilder contextBuilder;

    private final CashTools cashTools;
    private final CardTools cardTools;
    private final DebtTools debtTools;
    private final SummaryTools summaryTools;

    @Override
    public String chat(String userId, String message) {
        String systemPrompt = SystemPrompts.FINSIGHT_ADVISOR
                + "\n\n"
                + contextBuilder.buildFor(userId);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .tools(cashTools, cardTools, debtTools, summaryTools)
                .toolContext(Map.of(ToolContextKeys.USER_ID, userId))
                .call()
                .content();
    }
}