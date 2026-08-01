package dev.jbyanx.finsight.domain.financial.service.impl;

import dev.jbyanx.finsight.domain.card.dto.UtilizationStatus;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.debt.repository.FixedDebtRepository;
import dev.jbyanx.finsight.domain.financial.dto.*;
import dev.jbyanx.finsight.domain.financial.service.FinancialService;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import dev.jbyanx.finsight.domain.income.repository.IncomeSourceRepository;
import dev.jbyanx.finsight.domain.income.repository.OperationalExpenseRepository;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import dev.jbyanx.finsight.domain.invoice.repository.InvoicePaymentRepository;
import dev.jbyanx.finsight.domain.invoice.repository.InvoiceRepository;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialServiceImpl implements FinancialService {

    /** Cuántos días hacia adelante mira la alerta de facturas próximas. */
    private static final int UPCOMING_INVOICE_WINDOW_DAYS = 7;

    private final CreditCardRepository creditCardRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CashRepository cashRepository;
    private final FixedDebtRepository fixedDebtRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final OperationalExpenseRepository operationalExpenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;

    @Override
    public FinancialSummaryResponse getSummary(String userId) {
        // --- Tarjetas ---
        List<CreditCard> cards = creditCardRepository
                .findAllByUserIdAndActiveTrue(userId);

        BigDecimal totalLimit = cards.stream()
                .map(CreditCard::getCreditLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUsed = cards.stream()
                .map(CreditCard::getUsedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAvailableCredit = totalLimit.subtract(totalUsed);

        BigDecimal overallUtilization = totalLimit.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalUsed.multiply(BigDecimal.valueOf(100))
                .divide(totalLimit, 2, RoundingMode.HALF_UP);

        List<CardAlertResponse> cardAlerts = cards.stream()
                .filter(c -> c.isYellowAlert() || c.isRedAlert())
                .map(c -> CardAlertResponse.builder()
                        .cardId(c.getId())
                        .institution(c.getInstitution())
                        .nickname(c.getNickname())
                        .utilizationPercentage(c.getUtilizationPercentage())
                        .status(c.isRedAlert()
                                ? UtilizationStatus.RED
                                : UtilizationStatus.YELLOW)
                        .recommendedPayment(calculateRecommendedPayment(c))
                        .build())
                .toList();

        // --- Cuentas y efectivo ---
        BigDecimal totalSavings = savingsAccountRepository
                .findAllByUserIdAndActiveTrue(userId).stream()
                .map(SavingsAccount::getOwnBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCash = cashRepository.findByUserId(userId)
                .map(c -> c.getBalance())
                .orElse(BigDecimal.ZERO);

        // --- Deudas ---
        BigDecimal totalDebt = fixedDebtRepository
                .sumRemainingBalanceByUserId(userId);

        BigDecimal totalMonthlyDebt = fixedDebtRepository
                .findAllByUserIdAndActiveTrue(userId).stream()
                .map(d -> d.getMonthlyPayment() != null ? d.getMonthlyPayment() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Ingresos y gastos operativos ---
        BigDecimal estimatedIncome = incomeSourceRepository
                .findAllByUserIdAndActiveTrue(userId).stream()
                .filter(s -> s.getEstimatedAmount() != null)
                .map(s -> normalizeToMonthly(s))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal estimatedExpenses = operationalExpenseRepository
                .findAllByUserIdAndActiveTrue(userId).stream()
                .map(e -> normalizeExpenseToMonthly(
                        e.getEstimatedAmount(), e.getFrequency(), e.getOccurrencesPerMonth()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Facturas próximas ---
        List<InvoiceAlertResponse> upcomingInvoices =
                buildUpcomingInvoices(userId, LocalDate.now());

        return FinancialSummaryResponse.builder()
                .totalCreditLimit(totalLimit)
                .totalUsedAmount(totalUsed)
                .totalAvailableCredit(totalAvailableCredit)
                .overallUtilizationPercentage(overallUtilization)
                .cardAlerts(cardAlerts)
                .totalSavingsBalance(totalSavings)
                .totalCashBalance(totalCash)
                .totalAvailableBalance(totalSavings.add(totalCash))
                .totalRemainingDebt(totalDebt)
                .totalMonthlyDebtPayment(totalMonthlyDebt)
                .estimatedMonthlyIncome(estimatedIncome)
                .estimatedMonthlyOperationalExpenses(estimatedExpenses)
                .estimatedNetIncome(estimatedIncome.subtract(estimatedExpenses))
                .upcomingInvoices(upcomingInvoices)
                .build();
    }

    /**
     * Facturas que vencen dentro de los próximos {@value #UPCOMING_INVOICE_WINDOW_DAYS}
     * días y que todavía NO tienen pago registrado para ese período.
     *
     * Dos cosas que antes estaban mal y por las que una factura pagada seguía
     * apareciendo como pendiente:
     *
     * 1. El vencimiento se calcula como FECHA REAL, no como día del mes. {@code dueDay}
     *    solo guarda el día, así que la ocurrencia que se está alertando puede caer en
     *    el mes siguiente: el 28 de agosto, una factura con {@code dueDay = 2} vence en
     *    SEPTIEMBRE. De esa fecha se deriva el período (mes, año) contra el que se busca
     *    el pago — usar el mes actual a secas escondería la factura de septiembre por
     *    un pago de agosto, y viceversa.
     *
     * 2. Se cruza contra {@code invoice_payments}. La consulta anterior solo miraba
     *    {@code dueDay} y {@code active}, así que no tenía forma de saber si estaba pagada.
     *
     * De paso queda arreglado el caso de {@code dueDay = 31}: se recorta al último día
     * del mes, así que en febrero vence el 28 en vez de no vencer nunca.
     */
    private List<InvoiceAlertResponse> buildUpcomingInvoices(String userId, LocalDate today) {
        LocalDate windowEnd = today.plusDays(UPCOMING_INVOICE_WINDOW_DAYS);

        record Upcoming(Invoice invoice, LocalDate dueDate) {}

        List<Upcoming> candidates = invoiceRepository
                .findAllByUserIdAndActiveTrue(userId).stream()
                .filter(i -> i.getDueDay() != null)
                .map(i -> new Upcoming(i, nextDueDate(i.getDueDay(), today)))
                .filter(u -> !u.dueDate().isBefore(today) && !u.dueDate().isAfter(windowEnd))
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> paidPeriods = invoicePaymentRepository
                .findPaidPeriods(
                        candidates.stream().map(u -> u.invoice().getId()).toList(),
                        PaymentStatus.PAID)
                .stream()
                .map(p -> periodKey(p.getInvoiceId(), p.getPeriodYear(), p.getPeriodMonth()))
                .collect(Collectors.toSet());

        return candidates.stream()
                .filter(u -> !paidPeriods.contains(periodKey(
                        u.invoice().getId(),
                        u.dueDate().getYear(),
                        u.dueDate().getMonthValue())))
                .map(u -> InvoiceAlertResponse.builder()
                        .invoiceId(u.invoice().getId())
                        .description(u.invoice().getDescription())
                        .amount(u.invoice().getAmount())
                        .dueDay(u.invoice().getDueDay())
                        .own(u.invoice().isOwn())
                        .responsibleName(u.invoice().getResponsibleName())
                        .build())
                .toList();
    }

    /**
     * Próxima fecha real de vencimiento a partir del día del mes. Si la ocurrencia de
     * este mes ya pasó, devuelve la del mes siguiente.
     */
    private static LocalDate nextDueDate(int dueDay, LocalDate today) {
        LocalDate thisMonth = occurrenceIn(YearMonth.from(today), dueDay);
        return thisMonth.isBefore(today)
                ? occurrenceIn(YearMonth.from(today).plusMonths(1), dueDay)
                : thisMonth;
    }

    /** El día se recorta al rango válido del mes: dueDay 31 en febrero es el 28 (o 29). */
    private static LocalDate occurrenceIn(YearMonth month, int dueDay) {
        int day = Math.min(Math.max(dueDay, 1), month.lengthOfMonth());
        return month.atDay(day);
    }

    private static String periodKey(String invoiceId, int year, int month) {
        return invoiceId + ":" + year + ":" + month;
    }

    // Cuánto abonar para bajar la utilización al 30%
    private BigDecimal calculateRecommendedPayment(CreditCard card) {
        BigDecimal idealUsed = card.getCreditLimit()
                .multiply(BigDecimal.valueOf(0.30));
        BigDecimal diff = card.getUsedAmount().subtract(idealUsed);
        return diff.compareTo(BigDecimal.ZERO) > 0
                ? diff.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal normalizeToMonthly(IncomeSource source) {
        if (source.getEstimatedAmount() == null) return BigDecimal.ZERO;
        // Un ingreso de una sola vez no es un patrón mensual — contarlo infla
        // artificialmente el promedio, como si se repitiera cada mes.
        if (source.getFrequency() == IncomeSource.IncomeFrequency.ONE_TIME) return BigDecimal.ZERO;
        if (source.getOccurrencesPerMonth() != null) {
            return source.getEstimatedAmount()
                    .multiply(BigDecimal.valueOf(source.getOccurrencesPerMonth()));
        }
        return switch (source.getFrequency()) {
            case PER_SHIFT -> source.getEstimatedAmount().multiply(BigDecimal.valueOf(12));
            case WEEKLY -> source.getEstimatedAmount().multiply(BigDecimal.valueOf(4));
            case BIWEEKLY -> source.getEstimatedAmount().multiply(BigDecimal.valueOf(2));
            case MONTHLY -> source.getEstimatedAmount();
            case ONE_TIME -> BigDecimal.ZERO; // ya cubierto arriba; el switch exige exhaustividad
            case null -> BigDecimal.ZERO;
        };
    }

    private BigDecimal normalizeExpenseToMonthly(BigDecimal amount, IncomeFrequency frequency,
                                                 Integer occurrencesPerMonth) {
        if (frequency == IncomeSource.IncomeFrequency.ONE_TIME) return BigDecimal.ZERO;
        if (occurrencesPerMonth != null) {
            return amount.multiply(BigDecimal.valueOf(occurrencesPerMonth));
        }
        return switch (frequency) {
            case PER_SHIFT -> amount.multiply(BigDecimal.valueOf(12));
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(4));
            case BIWEEKLY -> amount.multiply(BigDecimal.valueOf(2));
            case MONTHLY -> amount;
            case ONE_TIME -> BigDecimal.ZERO;
        };
    }
}