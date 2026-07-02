package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.context.FinancialContextBuilder;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.dto.UtilizationStatus;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadToolsTest {

    @Mock private CreditCardService creditCardService;
    @Mock private FixedDebtService fixedDebtService;
    @Mock private FinancialContextBuilder contextBuilder;

    @InjectMocks private CardTools cardTools;
    @InjectMocks private DebtTools debtTools;
    @InjectMocks private SummaryTools summaryTools;

    private static ToolContext ctx(String userId) {
        return new ToolContext(Map.of(ToolContextKeys.USER_ID, userId));
    }

    private static ToolContext emptyCtx() {
        return new ToolContext(Map.of());
    }

    // ---------- CardTools ----------

    @Test
    void listCreditCards_WithCards_ReturnsFormatted() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(
                CreditCardResponse.builder()
                        .institution("Nu").nickname("Nu")
                        .creditLimit(new BigDecimal("400000"))
                        .usedAmount(new BigDecimal("100000"))
                        .availableAmount(new BigDecimal("300000"))
                        .utilizationPercentage(new BigDecimal("25.00"))
                        .utilizationStatus(UtilizationStatus.YELLOW)
                        .cutOffDay(3).paymentDueDay(23)
                        .build()));

        String result = cardTools.listCreditCards(ctx("user-1"));

        assertTrue(result.contains("Nu"));
        assertTrue(result.contains("25.00"));
    }

    @Test
    void listCreditCards_Empty_SaysNone() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        assertTrue(cardTools.listCreditCards(ctx("user-1")).toLowerCase().contains("no tiene"));
    }

    @Test
    void listCardsByUtilization_NoUser_DoesNotQuery() {
        assertTrue(cardTools.listCardsByUtilization(emptyCtx())
                .toLowerCase().contains("no se pudo identificar"));
        verify(creditCardService, never()).getOrderedByUtilization(anyString());
    }

    // ---------- DebtTools ----------

    @Test
    void listDebts_WithDebts_ReturnsFormatted() {
        when(fixedDebtService.getAllByUser("user-1")).thenReturn(List.of(
                FixedDebtResponse.builder()
                        .institution("Banco de Bogotá").description("Moto NKD")
                        .totalAmount(new BigDecimal("3000000"))
                        .remainingBalance(new BigDecimal("1200000"))
                        .monthlyPayment(new BigDecimal("250000"))
                        .paymentDueDay(15)
                        .progressPercentage(new BigDecimal("60.00"))
                        .paidOff(false)
                        .build()));

        String result = debtTools.listDebts(ctx("user-1"));

        assertTrue(result.contains("Moto NKD"));
        assertTrue(result.contains("1200000"));
    }

    @Test
    void listDebts_NoUser_DoesNotQuery() {
        assertTrue(debtTools.listDebts(emptyCtx())
                .toLowerCase().contains("no se pudo identificar"));
        verify(fixedDebtService, never()).getAllByUser(anyString());
    }

    // ---------- SummaryTools ----------

    @Test
    void getFinancialSummary_WithUser_DelegatesToBuilder() {
        when(contextBuilder.buildFor("user-1")).thenReturn("RESUMEN FINANCIERO...");
        assertEquals("RESUMEN FINANCIERO...", summaryTools.getFinancialSummary(ctx("user-1")));
    }

    @Test
    void getFinancialSummary_NoUser_DoesNotQuery() {
        assertTrue(summaryTools.getFinancialSummary(emptyCtx())
                .toLowerCase().contains("no se pudo identificar"));
        verify(contextBuilder, never()).buildFor(anyString());
    }
}