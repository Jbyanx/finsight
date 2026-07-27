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
import dev.jbyanx.finsight.domain.invoice.repository.InvoiceRepository;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialServiceImpl implements FinancialService {

    private final CreditCardRepository creditCardRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CashRepository cashRepository;
    private final FixedDebtRepository fixedDebtRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final OperationalExpenseRepository operationalExpenseRepository;
    private final InvoiceRepository invoiceRepository;

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
                        e.getEstimatedAmount(), e.getFrequency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Facturas próximas (próximos 7 días) ---
        int today = LocalDate.now().getDayOfMonth();
        int sevenDaysLater = LocalDate.now().plusDays(7).getDayOfMonth();

        List<InvoiceAlertResponse> upcomingInvoices = invoiceRepository
                .findUpcomingByUserId(userId, today, sevenDaysLater).stream()
                .map(i -> InvoiceAlertResponse.builder()
                        .invoiceId(i.getId())
                        .description(i.getDescription())
                        .amount(i.getAmount())
                        .dueDay(i.getDueDay())
                        .own(i.isOwn())
                        .responsibleName(i.getResponsibleName())
                        .build())
                .toList();

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
        if (source.getOccurrencesPerMonth() != null) {
            return source.getEstimatedAmount()
                    .multiply(BigDecimal.valueOf(source.getOccurrencesPerMonth()));
        }
        // fallback si no especifica occurrences
        return switch (source.getFrequency()) {
            case PER_SHIFT -> source.getEstimatedAmount().multiply(BigDecimal.valueOf(12));
            case WEEKLY -> source.getEstimatedAmount().multiply(BigDecimal.valueOf(4));
            case MONTHLY -> source.getEstimatedAmount();
            case null -> BigDecimal.ZERO;
        };
    }

    private BigDecimal normalizeExpenseToMonthly(BigDecimal amount,
                                                 IncomeFrequency frequency) {
        return switch (frequency) {
            case PER_SHIFT -> amount.multiply(BigDecimal.valueOf(12));
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(4));
            case MONTHLY -> amount;
        };
    }
}