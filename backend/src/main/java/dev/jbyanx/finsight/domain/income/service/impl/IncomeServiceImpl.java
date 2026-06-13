package dev.jbyanx.finsight.domain.income.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.income.dto.*;
import dev.jbyanx.finsight.domain.income.entity.*;
import dev.jbyanx.finsight.domain.income.mapper.IncomeMapper;
import dev.jbyanx.finsight.domain.income.repository.*;
import dev.jbyanx.finsight.domain.income.service.IncomeService;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.PocketRepository;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeServiceImpl implements IncomeService {

    private final IncomeSourceRepository incomeSourceRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final OperationalExpenseRepository operationalExpenseRepository;
    private final UserRepository userRepository;
    private final CashRepository cashRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final PocketRepository pocketRepository;
    private final IncomeMapper incomeMapper;

    // --- Fuentes de ingreso ---

    @Override
    @Transactional
    public IncomeSourceResponse createSource(String userId, IncomeSourceRequest request) {
        User user = findUser(userId);

        IncomeSource source = IncomeSource.builder()
                .user(user)
                .description(request.getDescription())
                .type(request.getType())
                .estimatedAmount(request.getEstimatedAmount())
                .frequency(request.getFrequency())
                .build();

        return incomeMapper.toSourceResponse(incomeSourceRepository.save(source));
    }

    @Override
    public List<IncomeSourceResponse> getAllSources(String userId) {
        return incomeMapper.toSourceResponseList(
                incomeSourceRepository.findAllByUserIdAndActiveTrue(userId));
    }

    @Override
    @Transactional
    public IncomeSourceResponse updateSource(String userId, String sourceId,
                                             IncomeSourceRequest request) {
        IncomeSource source = incomeSourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("IncomeSource", sourceId));

        source.setDescription(request.getDescription());
        source.setType(request.getType());
        source.setEstimatedAmount(request.getEstimatedAmount());
        source.setFrequency(request.getFrequency());

        return incomeMapper.toSourceResponse(incomeSourceRepository.save(source));
    }

    @Override
    @Transactional
    public void deleteSource(String userId, String sourceId) {
        IncomeSource source = incomeSourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("IncomeSource", sourceId));
        source.setActive(false);
        incomeSourceRepository.save(source);
    }

    // --- Registros de ingreso ---

    @Override
    @Transactional
    public IncomeEntryResponse registerIncome(String userId, IncomeEntryRequest request) {
        User user = findUser(userId);

        IncomeSource source = null;
        if (request.getIncomeSourceId() != null) {
            source = incomeSourceRepository.findByIdAndUserId(request.getIncomeSourceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "IncomeSource", request.getIncomeSourceId()));
        }

        // Actualiza el balance del destino
        SavingsAccount account = null;
        Pocket pocket = null;

        switch (request.getDestinationType()) {
            case CASH -> {
                Cash cash = cashRepository.findByUserId(userId)
                        .orElseGet(() -> cashRepository.save(
                                Cash.builder().user(user).build()));
                cash.add(request.getAmount());
                cashRepository.save(cash);
            }
            case SAVINGS -> {
                account = savingsAccountRepository
                        .findByIdAndUserId(request.getSavingsAccountId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "SavingsAccount", request.getSavingsAccountId()));
                account.setBalance(account.getBalance().add(request.getAmount()));
                savingsAccountRepository.save(account);
            }
            case POCKET -> {
                pocket = pocketRepository
                        .findByIdAndSavingsAccountId(request.getPocketId(),
                                request.getSavingsAccountId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Pocket", request.getPocketId()));
                pocket.setBalance(pocket.getBalance().add(request.getAmount()));
                pocketRepository.save(pocket);

                // Actualiza también el balance de la cuenta padre
                account = pocket.getSavingsAccount();
                account.setBalance(account.getBalance().add(request.getAmount()));
                savingsAccountRepository.save(account);
            }
        }

        IncomeEntry entry = IncomeEntry.builder()
                .user(user)
                .incomeSource(source)
                .amount(request.getAmount())
                .description(request.getDescription())
                .receivedAt(request.getReceivedAt())
                .destinationType(request.getDestinationType())
                .savingsAccount(account)
                .pocket(pocket)
                .build();

        return incomeMapper.toEntryResponse(incomeEntryRepository.save(entry));
    }

    @Override
    public Page<IncomeEntryResponse> getIncomeHistory(String userId, Pageable pageable) {
        return incomeEntryRepository
                .findAllByUserIdOrderByReceivedAtDesc(userId, pageable)
                .map(incomeMapper::toEntryResponse);
    }

    // --- Gastos operativos ---

    @Override
    @Transactional
    public OperationalExpenseResponse createExpense(String userId,
                                                    OperationalExpenseRequest request) {
        User user = findUser(userId);

        IncomeSource source = null;
        if (request.getIncomeSourceId() != null) {
            source = incomeSourceRepository.findByIdAndUserId(request.getIncomeSourceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "IncomeSource", request.getIncomeSourceId()));
        }

        OperationalExpense expense = OperationalExpense.builder()
                .user(user)
                .incomeSource(source)
                .description(request.getDescription())
                .frequency(request.getFrequency())
                .estimatedAmount(request.getEstimatedAmount())
                .build();

        return incomeMapper.toExpenseResponse(
                operationalExpenseRepository.save(expense));
    }

    @Override
    public List<OperationalExpenseResponse> getAllExpenses(String userId) {
        return incomeMapper.toExpenseResponseList(
                operationalExpenseRepository.findAllByUserIdAndActiveTrue(userId));
    }

    @Override
    @Transactional
    public OperationalExpenseResponse updateExpense(String userId, String expenseId,
                                                    OperationalExpenseRequest request) {
        OperationalExpense expense = operationalExpenseRepository
                .findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OperationalExpense", expenseId));

        expense.setDescription(request.getDescription());
        expense.setFrequency(request.getFrequency());
        expense.setEstimatedAmount(request.getEstimatedAmount());

        return incomeMapper.toExpenseResponse(
                operationalExpenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void deleteExpense(String userId, String expenseId) {
        OperationalExpense expense = operationalExpenseRepository
                .findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OperationalExpense", expenseId));
        expense.setActive(false);
        operationalExpenseRepository.save(expense);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}