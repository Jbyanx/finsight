package dev.jbyanx.finsight.domain.ai.pending;

import dev.jbyanx.finsight.domain.debt.dto.DebtPaymentRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import dev.jbyanx.finsight.domain.transaction.service.TransactionService;
import dev.jbyanx.finsight.shared.util.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ejecuta una {@link PendingAction} ya confirmada, contra el service real.
 *
 * SEGURIDAD: única vía por la que una mutación del bot llega a la BD, y la invoca
 * el CÓDIGO (ConfirmationHandler) tras una confirmación explícita — nunca el modelo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingActionExecutor {

    public static final String PAY_DEBT = "PAY_DEBT";
    public static final String REGISTER_TRANSACTION = "REGISTER_TRANSACTION";

    private final FixedDebtService fixedDebtService;
    private final TransactionService transactionService;

    public String execute(String userId, PendingAction action) {
        return switch (action.getType()) {
            case PAY_DEBT -> payDebt(userId, action);
            case REGISTER_TRANSACTION -> registerTransaction(userId, action);
            default -> {
                log.warn("Unknown pending action type: {}", action.getType());
                yield "No pude completar la acción; intenta de nuevo.";
            }
        };
    }

    private String payDebt(String userId, PendingAction action) {
        BigDecimal amount = new BigDecimal(action.param("amount"));

        FixedDebtResponse result = fixedDebtService.applyPayment(
                userId, action.param("debtId"),
                DebtPaymentRequest.builder().amount(amount).build());

        if (result.isPaidOff()) {
            return "Listo. Pagaste %s y saldaste por completo la deuda de %s."
                    .formatted(Money.format(amount), result.getInstitution());
        }
        return ("Listo. Registré el pago de %s. Te queda un saldo de %s en la deuda de %s "
                + "(próxima cuota el día %d).")
                .formatted(Money.format(amount), Money.format(result.getRemainingBalance()),
                        result.getInstitution(), result.getPaymentDueDay());
    }

    private String registerTransaction(String userId, PendingAction action) {
        String installmentsRaw = action.param("installments");

        TransactionRequest request = TransactionRequest.builder()
                .description(action.param("description"))
                .amount(new BigDecimal(action.param("amount")))
                .category(action.param("category"))
                .originType(OriginType.valueOf(action.param("originType")))
                .creditCardId(action.param("creditCardId"))
                .savingsAccountId(action.param("savingsAccountId"))
                .pocketId(action.param("pocketId"))
                .installments(installmentsRaw != null ? Integer.valueOf(installmentsRaw) : 1)
                .build();

        TransactionResponse tx = transactionService.create(userId, request);

        // Si se pagó con plata ajena, dejarlo explícito en la confirmación.
        if (tx.isThirdParty()) {
            return "Listo. Registré \"%s\" por %s con el dinero de %s. No afecta tu patrimonio."
                    .formatted(tx.getDescription(), Money.format(tx.getAmount()),
                            tx.getThirdPartyOwnerName());
        }

        String origin = tx.getOriginName() != null ? " con " + tx.getOriginName() : "";
        return "Listo. Registré \"%s\" por %s%s."
                .formatted(tx.getDescription(), Money.format(tx.getAmount()), origin);
    }
}