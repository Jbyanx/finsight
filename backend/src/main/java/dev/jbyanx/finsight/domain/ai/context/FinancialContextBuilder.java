package dev.jbyanx.finsight.domain.ai.context;

import dev.jbyanx.finsight.domain.financial.dto.CardAlertResponse;
import dev.jbyanx.finsight.domain.financial.dto.FinancialSummaryResponse;
import dev.jbyanx.finsight.domain.financial.dto.InvoiceAlertResponse;
import dev.jbyanx.finsight.domain.financial.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Construye el bloque de CONTEXTO FINANCIERO del usuario que se inyecta en el
 * system prompt, a partir de su resumen real (calculado por usuario desde BD).
 *
 * IMPORTANTE — multi-tenant: este texto se arma por userId en cada conversación.
 * No hay nada hardcodeado de ningún usuario en particular. Lo "personal" (que el
 * ahorro sea de un familiar, las metas, etc.) emerge de SUS datos: getSummary ya
 * excluye el ahorro compartido del disponible, etc.
 */
@Component
@RequiredArgsConstructor
public class FinancialContextBuilder {

    private final FinancialService financialService;

    public String buildFor(String userId) {
        FinancialSummaryResponse s = financialService.getSummary(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("CONTEXTO FINANCIERO ACTUAL DEL USUARIO\n");
        sb.append("(datos reales al día de hoy; los montos están en pesos)\n\n");

        sb.append("Tarjetas de crédito:\n");
        sb.append("- Cupo total: ").append(s.getTotalCreditLimit()).append("\n");
        sb.append("- Usado: ").append(s.getTotalUsedAmount()).append("\n");
        sb.append("- Disponible: ").append(s.getTotalAvailableCredit()).append("\n");
        sb.append("- Utilización global: ")
                .append(s.getOverallUtilizationPercentage()).append("%\n");

        if (s.getCardAlerts() != null && !s.getCardAlerts().isEmpty()) {
            sb.append("- Tarjetas en alerta de utilización:\n");
            for (CardAlertResponse c : s.getCardAlerts()) {
                sb.append("    • ").append(nameOf(c))
                        .append(": ").append(c.getUtilizationPercentage()).append("% (")
                        .append(c.getStatus()).append("). Abono sugerido para bajar a 30%: ")
                        .append(c.getRecommendedPayment()).append("\n");
            }
        }

        sb.append("\nLiquidez:\n");
        sb.append("- Efectivo: ").append(s.getTotalCashBalance()).append("\n");
        sb.append("- Ahorro disponible (excluye ahorro compartido): ")
                .append(s.getTotalSavingsBalance()).append("\n");
        sb.append("- Disponible total: ").append(s.getTotalAvailableBalance()).append("\n");

        sb.append("\nDeudas fijas:\n");
        sb.append("- Saldo total restante: ").append(s.getTotalRemainingDebt()).append("\n");
        sb.append("- Cuota mensual total: ").append(s.getTotalMonthlyDebtPayment()).append("\n");

        sb.append("\nFlujo mensual estimado:\n");
        sb.append("- Ingreso bruto: ").append(s.getEstimatedMonthlyIncome()).append("\n");
        sb.append("- Gastos operativos: ")
                .append(s.getEstimatedMonthlyOperationalExpenses()).append("\n");
        sb.append("- Ingreso NETO real: ").append(s.getEstimatedNetIncome()).append("\n");

        if (s.getUpcomingInvoices() != null && !s.getUpcomingInvoices().isEmpty()) {
            sb.append("\nFacturas próximas a vencer (7 días):\n");
            for (InvoiceAlertResponse i : s.getUpcomingInvoices()) {
                sb.append("    • ").append(i.getDescription())
                        .append(": ").append(i.getAmount())
                        .append(", vence día ").append(i.getDueDay());
                if (!i.isOwn() && i.getResponsibleName() != null) {
                    sb.append(" (la paga/reembolsa ").append(i.getResponsibleName()).append(")");
                }
                sb.append("\n");
            }
        } else {
            sb.append("\nFacturas próximas a vencer (7 días): ninguna.\n");
        }

        return sb.toString();
    }

    private String nameOf(CardAlertResponse c) {
        return c.getNickname() != null && !c.getNickname().isBlank()
                ? c.getNickname()
                : c.getInstitution();
    }
}