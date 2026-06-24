package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tools de efectivo expuestas al modelo (function calling).
 *
 * SEGURIDAD — el userId:
 * El método recibe un {@link ToolContext}, que es un parámetro que el modelo
 * NO ve (no aparece en el esquema que se le ofrece a Gemini) y NO puede
 * rellenar. El userId se inyecta del lado servidor vía .toolContext(...) al
 * invocar el ChatClient, y se lee aquí desde ese contexto.
 *
 * Esto respeta la convención: el userId SIEMPRE viene del principal/chatId
 * autenticado, NUNCA del input del modelo. Si Gemini intentara colar un userId
 * como argumento, no tendría dónde: la firma visible para él no lo incluye.
 */
@Component
@RequiredArgsConstructor
public class CashTools {

    private final CashService cashService;

    @Tool(description = "Consulta el saldo de efectivo actual del usuario. "
            + "Úsala cuando el usuario pregunte cuánto dinero en efectivo tiene.")
    public String getCashBalance(ToolContext toolContext) {
        String userId = (String) toolContext.getContext().get(ToolContextKeys.USER_ID);
        if (userId == null) {
            // Salvaguarda: si no hay userId en contexto, no consultamos nada.
            return "No se pudo identificar al usuario.";
        }

        CashResponse cash = cashService.getByUser(userId);
        return "Saldo de efectivo actual: " + cash.getBalance();
    }
}