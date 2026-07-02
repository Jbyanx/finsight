package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.context.FinancialContextBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool de LECTURA del resumen financiero completo.
 *
 * Reusa FinancialContextBuilder (el del #52), que ya arma el panorama por usuario
 * desde getSummary. Aunque ese contexto ya va en el system prompt, exponerlo como
 * tool permite al modelo "refrescarlo" explícitamente si lo necesita en medio de
 * una conversación (ej. tras registrar algo, en el futuro).
 */
@Component
@RequiredArgsConstructor
public class SummaryTools {

    private final FinancialContextBuilder contextBuilder;

    @Tool(description = "Devuelve el resumen financiero completo y actualizado del "
            + "usuario: tarjetas, utilización, liquidez, deudas, ingreso neto y "
            + "facturas próximas. Úsala cuando el usuario pida un panorama general "
            + "de su situación o un resumen de sus finanzas.")
    public String getFinancialSummary(ToolContext toolContext) {
        String userId = (String) toolContext.getContext().get(ToolContextKeys.USER_ID);
        if (userId == null) return "No se pudo identificar al usuario.";
        return contextBuilder.buildFor(userId);
    }
}