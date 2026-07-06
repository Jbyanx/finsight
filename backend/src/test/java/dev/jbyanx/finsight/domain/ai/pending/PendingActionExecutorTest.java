package dev.jbyanx.finsight.domain.ai.pending;

import dev.jbyanx.finsight.domain.debt.dto.DebtPaymentRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
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
    @InjectMocks private PendingActionExecutor executor;

    private PendingAction payAction() {
        return PendingAction.of(PendingActionExecutor.PAY_DEBT, null)
                .with("debtId", "d-1").with("amount", "250000");
    }

    @Test
    void execute_PayDebt_Partial_ReturnsRemaining() {
        when(fixedDebtService.applyPayment(eq("user-1"), eq("d-1"), any(DebtPaymentRequest.class)))
                .thenReturn(FixedDebtResponse.builder()
                        .institution("Banco de Bogotá")
                        .remainingBalance(new BigDecimal("950000"))
                        .paymentDueDay(15)
                        .paidOff(false)
                        .build());

        String result = executor.execute("user-1", payAction());

        assertTrue(result.contains("950000"));
        assertTrue(result.contains("15"));
    }

    @Test
    void execute_PayDebt_PaidOff_Celebrates() {
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
    void execute_UnknownType_ReturnsSafeMessage() {
        String result = executor.execute("user-1",
                PendingAction.of("UNKNOWN_TYPE", null));

        assertTrue(result.toLowerCase().contains("no pude"));
    }
}