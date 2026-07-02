package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tools de LECTURA de tarjetas de crédito.
 *
 * Mismo patrón de seguridad que CashTools: el userId se lee del ToolContext
 * (canal del servidor), nunca lo provee el modelo.
 *
 * Las tools devuelven TEXTO ya formateado (no objetos crudos): el modelo redacta
 * mejor a partir de una descripción legible que de JSON.
 */
@Component
@RequiredArgsConstructor
public class CardTools {

    private final CreditCardService creditCardService;

    @Tool(description = "Lista las tarjetas de crédito del usuario con su cupo, "
            + "monto usado, disponible y porcentaje de utilización. Úsala cuando "
            + "el usuario pregunte por sus tarjetas o su utilización.")
    public String listCreditCards(ToolContext toolContext) {
        String userId = userId(toolContext);
        if (userId == null) return "No se pudo identificar al usuario.";

        List<CreditCardResponse> cards = creditCardService.getAllByUser(userId);
        if (cards.isEmpty()) {
            return "El usuario no tiene tarjetas de crédito registradas.";
        }
        return format(cards);
    }

    @Tool(description = "Lista las tarjetas ordenadas de MAYOR a menor utilización. "
            + "Úsala cuando el usuario pregunte con qué tarjeta pagar o cuál tiene "
            + "más cupo disponible: la de menor utilización es la mejor para usar.")
    public String listCardsByUtilization(ToolContext toolContext) {
        String userId = userId(toolContext);
        if (userId == null) return "No se pudo identificar al usuario.";

        List<CreditCardResponse> cards = creditCardService.getOrderedByUtilization(userId);
        if (cards.isEmpty()) {
            return "El usuario no tiene tarjetas de crédito registradas.";
        }
        return format(cards);
    }

    private String format(List<CreditCardResponse> cards) {
        StringBuilder sb = new StringBuilder("Tarjetas de crédito del usuario:\n");
        for (CreditCardResponse c : cards) {
            String name = (c.getNickname() != null && !c.getNickname().isBlank())
                    ? c.getNickname() : c.getInstitution();
            sb.append("- ").append(name)
                    .append(": cupo ").append(c.getCreditLimit())
                    .append(", usado ").append(c.getUsedAmount())
                    .append(", disponible ").append(c.getAvailableAmount())
                    .append(", utilización ").append(c.getUtilizationPercentage())
                    .append("% (").append(c.getUtilizationStatus()).append(")")
                    .append(", corte día ").append(c.getCutOffDay())
                    .append(", pago día ").append(c.getPaymentDueDay())
                    .append("\n");
        }
        return sb.toString();
    }

    private String userId(ToolContext ctx) {
        return (String) ctx.getContext().get(ToolContextKeys.USER_ID);
    }
}