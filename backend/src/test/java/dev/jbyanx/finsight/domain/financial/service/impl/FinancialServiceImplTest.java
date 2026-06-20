package dev.jbyanx.finsight.domain.financial.service.impl;

import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.financial.dto.FinancialSummaryResponse;
import dev.jbyanx.finsight.domain.debt.repository.FixedDebtRepository;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import dev.jbyanx.finsight.domain.income.entity.OperationalExpense;
import dev.jbyanx.finsight.domain.income.repository.IncomeSourceRepository;
import dev.jbyanx.finsight.domain.income.repository.OperationalExpenseRepository;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import dev.jbyanx.finsight.domain.invoice.repository.InvoiceRepository;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialServiceImplTest {

    @Mock private CreditCardRepository creditCardRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private CashRepository cashRepository;
    @Mock private FixedDebtRepository fixedDebtRepository;
    @Mock private IncomeSourceRepository incomeSourceRepository;
    @Mock private OperationalExpenseRepository operationalExpenseRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private FinancialServiceImpl financialService;

    private final String USER_ID = "user-1";

    @Test
    void getSummary_HappyPath_CalculatesEverythingCorrectly() {
        // 1. Setup Tarjetas (Límite 400k, Uso 200k -> 50% utilización, debe ser alerta roja)
        CreditCard card = CreditCard.builder()
                .id("card-1")
                .institution("Nu")
                .creditLimit(new BigDecimal("400000"))
                .usedAmount(new BigDecimal("200000")) // 50%
                .build();
        when(creditCardRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(card));

        // 2. Setup Ahorros y Efectivo (Ahorro 50k, Efectivo 20k)
        SavingsAccount account = SavingsAccount.builder()
                .balance(new BigDecimal("50000")).shared(false).build();
        when(savingsAccountRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(account));

        Cash cash = Cash.builder().balance(new BigDecimal("20000")).build();
        when(cashRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cash));

        // 3. Setup Deudas (Total 1M, Cuota 100k)
        when(fixedDebtRepository.sumRemainingBalanceByUserId(USER_ID))
                .thenReturn(new BigDecimal("1000000"));

        dev.jbyanx.finsight.domain.debt.entity.FixedDebt debt =
                dev.jbyanx.finsight.domain.debt.entity.FixedDebt.builder()
                        .monthlyPayment(new BigDecimal("100000")).build();
        when(fixedDebtRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(debt));

        // 4. Setup Ingresos y Gastos
        IncomeSource income = IncomeSource.builder()
                .estimatedAmount(new BigDecimal("60000"))
                .frequency(IncomeFrequency.PER_SHIFT).build(); // 60k * 12 = 720k mensual
        when(incomeSourceRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(income));

        OperationalExpense expense = OperationalExpense.builder()
                .estimatedAmount(new BigDecimal("20000"))
                .frequency(IncomeFrequency.PER_SHIFT).build(); // 20k * 12 = 240k mensual
        when(operationalExpenseRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(expense));

        // 5. Setup Facturas
        Invoice invoice = Invoice.builder().id("inv-1").amount(new BigDecimal("60000")).own(true).build();
        // Usamos anyInt() para que el test no falle cuando cambie el día del mes
        when(invoiceRepository.findUpcomingByUserId(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(List.of(invoice));

        // EJECUCIÓN
        FinancialSummaryResponse response = financialService.getSummary(USER_ID);

        // ASERCIONES
        assertNotNull(response);

        // Tarjetas
        assertEquals(0, new BigDecimal("400000").compareTo(response.getTotalCreditLimit()));
        assertEquals(0, new BigDecimal("200000").compareTo(response.getTotalAvailableCredit()));
        assertEquals(0, new BigDecimal("50.00").compareTo(response.getOverallUtilizationPercentage()));
        assertEquals(1, response.getCardAlerts().size());

        // La recomendación de pago para bajar al 30% (Ideal: 400k * 0.3 = 120k. Usado 200k. Pago = 80k)
        assertEquals(0, new BigDecimal("80000.00").compareTo(response.getCardAlerts().get(0).getRecommendedPayment()));

        // Liquidez
        assertEquals(0, new BigDecimal("50000").compareTo(response.getTotalSavingsBalance()));
        assertEquals(0, new BigDecimal("20000").compareTo(response.getTotalCashBalance()));
        assertEquals(0, new BigDecimal("70000").compareTo(response.getTotalAvailableBalance()));

        // Deudas
        assertEquals(0, new BigDecimal("1000000").compareTo(response.getTotalRemainingDebt()));
        assertEquals(0, new BigDecimal("100000").compareTo(response.getTotalMonthlyDebtPayment()));

        // Flujo de Caja
        assertEquals(0, new BigDecimal("720000").compareTo(response.getEstimatedMonthlyIncome()));
        assertEquals(0, new BigDecimal("240000").compareTo(response.getEstimatedMonthlyOperationalExpenses()));
        assertEquals(0, new BigDecimal("480000").compareTo(response.getEstimatedNetIncome())); // 720k - 240k

        // Facturas
        assertEquals(1, response.getUpcomingInvoices().size());
    }

    @Test
    void getSummary_EmptyState_ReturnsAllZeros() {
        // Configuramos todos los repositorios para devolver vacío o null
        when(creditCardRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(Collections.emptyList());
        when(savingsAccountRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(Collections.emptyList());
        when(cashRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(fixedDebtRepository.sumRemainingBalanceByUserId(USER_ID)).thenReturn(null); // O BigDecimal.ZERO si tu query lo garantiza
        when(fixedDebtRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(Collections.emptyList());
        when(incomeSourceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(Collections.emptyList());
        when(operationalExpenseRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(Collections.emptyList());
        when(invoiceRepository.findUpcomingByUserId(eq(USER_ID), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        FinancialSummaryResponse response = financialService.getSummary(USER_ID);

        // Aserciones para el Empty State
        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalCreditLimit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getOverallUtilizationPercentage()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getEstimatedNetIncome()));
        assertTrue(response.getCardAlerts().isEmpty());
        assertTrue(response.getUpcomingInvoices().isEmpty());
    }
}