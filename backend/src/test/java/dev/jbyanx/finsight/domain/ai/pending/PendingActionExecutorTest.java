package dev.jbyanx.finsight.domain.ai.pending;

import dev.jbyanx.finsight.domain.debt.dto.DebtPaymentRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import dev.jbyanx.finsight.domain.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingActionExecutorTest {

    @Mock private FixedDebtService fixedDebtService;
    @Mock private TransactionService transactionService;
    @InjectMocks private PendingActionExecutor executor;

    private PendingAction payAction() {
        return PendingAction.of(PendingActionExecutor.PAY_DEBT, null)
                .with("debtId", "d-1").with("amount", "250000");
    }

    @Test
    void execute_PayDebt_Partial_ReturnsFormattedRemaining() {
        when(fixedDebtService.applyPayment(eq("user-1"), eq("d-1"), any(DebtPaymentRequest.class)))
                .thenReturn(FixedDebtResponse.builder()
                        .institution("Banco de Bogotá")
                        .remainingBalance(new BigDecimal("950000"))
                        .paymentDueDay(15)
                        .paidOff(false)
                        .build());

        String result = executor.execute("user-1", payAction());

        assertTrue(result.contains("$950.000"));
        assertTrue(result.contains("$250.000"));
        assertTrue(result.contains("15"));
    }

    @Test
    void execute_PayDebt_PaidOff_Confirms() {
        when(fixedDebtService.applyPayment(eq("user-1"), eq("d-1"), any(DebtPaymentRequest.class)))
                .thenReturn(FixedDebtResponse.builder()
                        .institution("Banco de Bogotá")
                        .remainingBalance(BigDecimal.ZERO)
                        .paidOff(true)
                        .build());

        String result = executor.execute("user-1", payAction());

        assertTrue(result.toLowerCase().contains("saldaste"));
    }

    @Test
    void execute_RegisterTransaction_CallsServiceAndFormatsAmount() {
        when(transactionService.create(eq("user-1"), any(TransactionRequest.class)))
                .thenReturn(TransactionResponse.builder()
                        .description("Par de tenis")
                        .amount(new BigDecimal("200000"))
                        .originName("NuCard")
                        .build());

        PendingAction action = PendingAction.of(PendingActionExecutor.REGISTER_TRANSACTION, null)
                .with("description", "Par de tenis")
                .with("amount", "200000")
                .with("originType", "CREDIT_CARD")
                .with("creditCardId", "card-1")
                .with("installments", "1");

        String result = executor.execute("user-1", action);

        assertTrue(result.contains("$200.000"));
        assertTrue(result.contains("Par de tenis"));
        assertTrue(result.contains("NuCard"));
    }

    @Test
    void execute_UnknownType_ReturnsSafeMessage() {
        String result = executor.execute("user-1",
                PendingAction.of("UNKNOWN_TYPE", null));

        assertTrue(result.toLowerCase().contains("no pude"));
    }
}