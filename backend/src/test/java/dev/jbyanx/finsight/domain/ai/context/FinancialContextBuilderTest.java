package dev.jbyanx.finsight.domain.ai.context;

import dev.jbyanx.finsight.domain.financial.dto.CardAlertResponse;
import dev.jbyanx.finsight.domain.financial.dto.FinancialSummaryResponse;
import dev.jbyanx.finsight.domain.financial.dto.InvoiceAlertResponse;
import dev.jbyanx.finsight.domain.card.dto.UtilizationStatus;
import dev.jbyanx.finsight.domain.financial.service.FinancialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialContextBuilderTest {

    @Mock
    private FinancialService financialService;

    @InjectMocks
    private FinancialContextBuilder builder;

    @Test
    void buildFor_IncludesKeyFiguresAndAlerts() {
        FinancialSummaryResponse summary = FinancialSummaryResponse.builder()
                .totalCreditLimit(new BigDecimal("3000000"))
                .totalUsedAmount(new BigDecimal("900000"))
                .totalAvailableCredit(new BigDecimal("2100000"))
                .overallUtilizationPercentage(new BigDecimal("30.00"))
                .cardAlerts(List.of(CardAlertResponse.builder()
                        .institution("Nu").nickname("Nu")
                        .utilizationPercentage(new BigDecimal("85.00"))
                        .status(UtilizationStatus.RED)
                        .recommendedPayment(new BigDecimal("220000"))
                        .build()))
                .totalCashBalance(new BigDecimal("110000"))
                .totalSavingsBalance(new BigDecimal("500000"))
                .totalAvailableBalance(new BigDecimal("610000"))
                .totalRemainingDebt(new BigDecimal("1200000"))
                .totalMonthlyDebtPayment(new BigDecimal("150000"))
                .estimatedMonthlyIncome(new BigDecimal("720000"))
                .estimatedMonthlyOperationalExpenses(new BigDecimal("276000"))
                .estimatedNetIncome(new BigDecimal("444000"))
                .upcomingInvoices(List.of(InvoiceAlertResponse.builder()
                        .description("Plan telefonía").amount(new BigDecimal("45000"))
                        .dueDay(15).own(true).build()))
                .build();

        when(financialService.getSummary("user-1")).thenReturn(summary);

        String ctx = builder.buildFor("user-1");

        // Cifras clave presentes
        assertTrue(ctx.contains("30.00"));      // utilización
        assertTrue(ctx.contains("444000"));     // ingreso neto
        assertTrue(ctx.contains("110000"));     // efectivo
        // Alerta de tarjeta con abono sugerido
        assertTrue(ctx.contains("Nu"));
        assertTrue(ctx.contains("220000"));
        // Factura próxima
        assertTrue(ctx.contains("Plan telefonía"));
    }

    @Test
    void buildFor_NoUpcomingInvoices_SaysNone() {
        FinancialSummaryResponse summary = FinancialSummaryResponse.builder()
                .totalCreditLimit(BigDecimal.ZERO).totalUsedAmount(BigDecimal.ZERO)
                .totalAvailableCredit(BigDecimal.ZERO)
                .overallUtilizationPercentage(BigDecimal.ZERO)
                .cardAlerts(List.of())
                .totalCashBalance(BigDecimal.ZERO).totalSavingsBalance(BigDecimal.ZERO)
                .totalAvailableBalance(BigDecimal.ZERO)
                .totalRemainingDebt(BigDecimal.ZERO).totalMonthlyDebtPayment(BigDecimal.ZERO)
                .estimatedMonthlyIncome(BigDecimal.ZERO)
                .estimatedMonthlyOperationalExpenses(BigDecimal.ZERO)
                .estimatedNetIncome(BigDecimal.ZERO)
                .upcomingInvoices(List.of())
                .build();

        when(financialService.getSummary("user-1")).thenReturn(summary);

        String ctx = builder.buildFor("user-1");

        assertTrue(ctx.toLowerCase().contains("ninguna"));
    }
}