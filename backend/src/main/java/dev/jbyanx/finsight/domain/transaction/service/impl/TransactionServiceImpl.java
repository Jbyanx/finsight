package dev.jbyanx.finsight.domain.transaction.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.PocketRepository;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import dev.jbyanx.finsight.domain.transaction.mapper.TransactionMapper;
import dev.jbyanx.finsight.domain.transaction.repository.TransactionRepository;
import dev.jbyanx.finsight.domain.transaction.service.TransactionService;
import dev.jbyanx.finsight.shared.exception.BusinessRuleException;
import dev.jbyanx.finsight.shared.exception.InsufficientFundsException;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CashRepository cashRepository;
    private final PocketRepository pocketRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransactionResponse create(String userId, TransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Resuelve el origen y actualiza su balance
        CreditCard creditCard = null;
        SavingsAccount savingsAccount = null;
        Pocket pocket = null;
        boolean thirdParty = false;
        String thirdPartyOwnerName = null;

        if (request.getPocketId() != null
                && request.getOriginType() != OriginType.SAVINGS) {
            throw new BusinessRuleException(
                    "pocketId is only valid when originType is SAVINGS");
        }

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
                    throw new InsufficientFundsException(
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

                if (request.getPocketId() != null) {
                    // --- Gasto DESDE UN BOLSILLO -------------------------------------
                    // Puede ser dinero propio reservado (bolsillo con meta) o dinero de un
                    // tercero ("paga la factura con la plata de tu papá"). En ambos casos el
                    // límite es el saldo DEL BOLSILLO, no el de la cuenta.
                    Pocket p = pocketRepository
                            .findByIdAndSavingsAccountId(
                                    request.getPocketId(), savingsAccount.getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Pocket", request.getPocketId()));

                    if (!p.isActive()) {
                        throw new BusinessRuleException("Pocket is not active: " + p.getName());
                    }

                    if (request.getAmount().compareTo(p.getBalance()) > 0) {
                        throw new InsufficientFundsException(
                                "Insufficient balance in pocket: " + p.getName());
                    }

                    // Descuenta del bolsillo Y del saldo físico de la cuenta.
                    // Si el bolsillo es compartido, ownBalance = balance - sharedTotal se
                    // mantiene igual: la plata salió del banco pero no era del usuario.
                    p.setBalance(p.getBalance().subtract(request.getAmount()));
                    pocketRepository.save(p);

                    savingsAccount.setBalance(
                            savingsAccount.getBalance().subtract(request.getAmount()));
                    savingsAccountRepository.save(savingsAccount);

                    pocket = p;
                    thirdParty = p.isShared();
                    thirdPartyOwnerName = p.isShared() ? p.getSharedOwnerName() : null;

                } else {
                    // --- Gasto NORMAL del usuario -------------------------------------
                    // Se valida contra el dinero LIBRE (getAvailableBalance), no contra el
                    // saldo físico: así es imposible gastar sin querer la plata de terceros
                    // ni vaciar un bolsillo con meta propia por accidente.
                    BigDecimal available = savingsAccount.getAvailableBalance();

                    if (request.getAmount().compareTo(available) > 0) {
                        throw new InsufficientFundsException(
                                "Insufficient available balance on account: "
                                        + savingsAccount.getInstitution()
                                        + ". Available: " + available
                                        + " (money in pockets is excluded)");
                    }

                    savingsAccount.setBalance(
                            savingsAccount.getBalance().subtract(request.getAmount()));
                    savingsAccountRepository.save(savingsAccount);
                }
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
                .savingsAccount(savingsAccount)
                .pocket(pocket)
                .thirdParty(thirdParty)
                .thirdPartyOwnerName(thirdPartyOwnerName)
                .category(request.getCategory())
                .date(request.getDate())
                .originType(request.getOriginType())
                .creditCard(creditCard)
                .shared(request.isShared())
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
                .findAllByUserIdAndSharedTrueOrderByDateDesc(userId, pageable)
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