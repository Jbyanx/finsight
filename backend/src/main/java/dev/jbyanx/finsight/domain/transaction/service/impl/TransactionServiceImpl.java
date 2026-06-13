package dev.jbyanx.finsight.domain.transaction.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import dev.jbyanx.finsight.domain.transaction.mapper.TransactionMapper;
import dev.jbyanx.finsight.domain.transaction.repository.TransactionRepository;
import dev.jbyanx.finsight.domain.transaction.service.TransactionService;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CashRepository cashRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransactionResponse create(String userId, TransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Resuelve el origen y actualiza su balance
        CreditCard creditCard = null;
        SavingsAccount savingsAccount = null;

        switch (request.getOriginType()) {

            case CREDIT_CARD -> {
                // Valida que la tarjeta pertenece al usuario
                creditCard = creditCardRepository
                        .findByIdAndUserId(request.getCreditCardId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "CreditCard", request.getCreditCardId()));

                // Verifica que hay cupo disponible
                if (creditCard.getUsedAmount().add(request.getAmount())
                        .compareTo(creditCard.getCreditLimit()) > 0) {
                    throw new UnauthorizedException(
                            "Insufficient credit limit on card: "
                                    + creditCard.getInstitution());
                }

                // Incrementa el saldo utilizado — aquí está el core financiero
                creditCard.setUsedAmount(
                        creditCard.getUsedAmount().add(request.getAmount()));
                creditCardRepository.save(creditCard);
            }

            case SAVINGS -> {
                savingsAccount = savingsAccountRepository
                        .findByIdAndUserId(request.getSavingsAccountId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "SavingsAccount", request.getSavingsAccountId()));

                // Verifica que hay saldo disponible
                if (request.getAmount().compareTo(savingsAccount.getBalance()) > 0) {
                    throw new UnauthorizedException(
                            "Insufficient balance on account: "
                                    + savingsAccount.getInstitution());
                }

                savingsAccount.setBalance(
                        savingsAccount.getBalance().subtract(request.getAmount()));
                savingsAccountRepository.save(savingsAccount);
            }

            case CASH -> {
                Cash cash = cashRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Cash", userId));
                // subtract ya valida que hay saldo suficiente
                cash.subtract(request.getAmount());
                cashRepository.save(cash);
            }
        }

        // Construye la transacción
        Transaction transaction = Transaction.builder()
                .user(user)
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(request.getCategory())
                .date(request.getDate())
                .originType(request.getOriginType())
                .creditCard(creditCard)
                .savingsAccount(savingsAccount)
                .isShared(request.isShared())
                .sharedWithName(request.getSharedWithName())
                .sharedAmount(request.getSharedAmount())
                .paymentDeadline(request.getPaymentDeadline())
                .installments(request.getInstallments() != null
                        ? request.getInstallments() : 1)
                .build();

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Override
    public Page<TransactionResponse> getAll(String userId, Pageable pageable) {
        return transactionRepository
                .findAllByUserIdOrderByDateDesc(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    public Page<TransactionResponse> getByCard(String userId, String cardId,
                                               Pageable pageable) {
        // Verifica que la tarjeta pertenece al usuario antes de listar
        creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditCard", cardId));

        return transactionRepository
                .findAllByUserIdAndCreditCardIdOrderByDateDesc(userId, cardId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    public Page<TransactionResponse> getShared(String userId, Pageable pageable) {
        return transactionRepository
                .findAllByUserIdAndIsSharedTrueOrderByDateDesc(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    public TransactionResponse getById(String userId, String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", transactionId));

        // Verifica que pertenece al usuario
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction", transactionId);
        }

        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public void delete(String userId, String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction", transactionId);
        }

        // Revierte el balance del origen al eliminar
        switch (transaction.getOriginType()) {
            case CREDIT_CARD -> {
                CreditCard card = transaction.getCreditCard();
                card.setUsedAmount(card.getUsedAmount()
                        .subtract(transaction.getAmount()));
                creditCardRepository.save(card);
            }
            case SAVINGS -> {
                SavingsAccount account = transaction.getSavingsAccount();
                account.setBalance(account.getBalance()
                        .add(transaction.getAmount()));
                savingsAccountRepository.save(account);
            }
            case CASH -> {
                cashRepository.findByUserId(userId).ifPresent(cash -> {
                    cash.add(transaction.getAmount());
                    cashRepository.save(cash);
                });
            }
        }

        transactionRepository.delete(transaction);
    }
}