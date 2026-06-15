package dev.jbyanx.finsight.domain.financial.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FinancialSummaryResponse {

    // Tarjetas
    private BigDecimal totalCreditLimit;
    private BigDecimal totalUsedAmount;
    private BigDecimal totalAvailableCredit;
    private BigDecimal overallUtilizationPercentage;
    private List<CardAlertResponse> cardAlerts;

    // Cuentas y efectivo
    private BigDecimal totalSavingsBalance;
    private BigDecimal totalCashBalance;
    private BigDecimal totalAvailableBalance;

    // Deudas
    private BigDecimal totalRemainingDebt;
    private BigDecimal totalMonthlyDebtPayment;

    // Ingresos y gastos operativos
    private BigDecimal estimatedMonthlyIncome;
    private BigDecimal estimatedMonthlyOperationalExpenses;
    private BigDecimal estimatedNetIncome;

    // Facturas
    private List<InvoiceAlertResponse> upcomingInvoices;
}