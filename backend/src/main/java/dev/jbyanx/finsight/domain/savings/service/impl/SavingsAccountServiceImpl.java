package dev.jbyanx.finsight.domain.savings.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.savings.dto.*;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.mapper.SavingsAccountMapper;
import dev.jbyanx.finsight.domain.savings.repository.PocketRepository;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import dev.jbyanx.finsight.domain.savings.service.SavingsAccountService;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.BusinessRuleException;
import dev.jbyanx.finsight.shared.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsAccountServiceImpl implements SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final PocketRepository pocketRepository;
    private final UserRepository userRepository;
    private final SavingsAccountMapper savingsAccountMapper;

    @Override
    @Transactional
    public SavingsAccountResponse create(String userId, SavingsAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        SavingsAccount account = SavingsAccount.builder()
                .user(user)
                .institution(request.getInstitution())
                .nickname(request.getNickname())
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .shared(request.isShared())
                .sharedOwnerName(request.getSharedOwnerName())
                .build();

        return savingsAccountMapper.toResponse(savingsAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsAccountResponse getById(String userId, String accountId) {
        return savingsAccountMapper.toResponse(findAccountBelongingToUser(userId, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsAccountResponse> getAllByUser(String userId) {
        return savingsAccountMapper.toResponseList(
                savingsAccountRepository.findAllByUserIdAndActiveTrue(userId)
        );
    }

    @Override
    @Transactional
    public SavingsAccountResponse update(String userId, String accountId,
                                         SavingsAccountRequest request) {
        SavingsAccount account = findAccountBelongingToUser(userId, accountId);

        account.setInstitution(request.getInstitution());
        account.setNickname(request.getNickname());
        account.setShared(request.isShared());
        account.setSharedOwnerName(request.getSharedOwnerName());

        return savingsAccountMapper.toResponse(savingsAccountRepository.save(account));
    }

    @Override
    @Transactional
    public SavingsAccountResponse patch(String userId, String accountId,
                                        SavingsAccountPatchRequest request) {
        SavingsAccount account = findAccountBelongingToUser(userId, accountId);

        if (request.getNickname() != null) account.setNickname(request.getNickname());
        if (request.getShared() != null) {
            account.setShared(request.getShared());
            // Regla de negocio: si deja de ser compartida, limpia el nombre del dueño
            if (!request.getShared()) account.setSharedOwnerName(null);
        }
        if (request.getSharedOwnerName() != null)
            account.setSharedOwnerName(request.getSharedOwnerName());

        return savingsAccountMapper.toResponse(savingsAccountRepository.save(account));
    }

    @Override
    @Transactional
    public PocketResponse patchPocket(String userId, String accountId,
                                      String pocketId, PocketPatchRequest request) {
        findAccountBelongingToUser(userId, accountId);

        Pocket pocket = pocketRepository.findByIdAndSavingsAccountId(pocketId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket", pocketId));

        if (request.getName() != null) pocket.setName(request.getName());
        if (request.isRemoveGoal()) {
            pocket.setGoalAmount(null);
        } else if (request.getGoalAmount() != null) {
            validateGoalNotBelowBalance(request.getGoalAmount(), pocket);
            pocket.setGoalAmount(request.getGoalAmount());
        }

        return savingsAccountMapper.toPocketResponse(pocketRepository.save(pocket));
    }

    @Override
    @Transactional
    public void delete(String userId, String accountId) {
        SavingsAccount account = findAccountBelongingToUser(userId, accountId);
        account.setActive(false);
        savingsAccountRepository.save(account);
    }

    @Override
    @Transactional
    public PocketResponse createPocket(String userId, String accountId,
                                       PocketRequest request) {
        SavingsAccount account = findAccountBelongingToUser(userId, accountId);

        BigDecimal initialBalance = request.getBalance() != null
                ? request.getBalance() : BigDecimal.ZERO;

        // Regla de negocio: un bolsillo RESERVA parte del disponible; no crea dinero.
        // Su saldo inicial no puede superar lo que está libre en la cuenta.
        BigDecimal available = account.getAvailableBalance();
        if (initialBalance.compareTo(available) > 0) {
            throw new InsufficientFundsException(
                    "Pocket balance exceeds available account balance: " + available);
        }

        Pocket pocket = Pocket.builder()
                .savingsAccount(account)
                .name(request.getName())
                .balance(initialBalance)
                .goalAmount(request.getGoalAmount())
                .build();

        return savingsAccountMapper.toPocketResponse(pocketRepository.save(pocket));
    }

    @Override
    @Transactional
    public PocketResponse updatePocket(String userId, String accountId,
                                       String pocketId, PocketRequest request) {
        findAccountBelongingToUser(userId, accountId);

        Pocket pocket = pocketRepository.findByIdAndSavingsAccountId(pocketId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket", pocketId));

        pocket.setName(request.getName());
        if (request.getGoalAmount() != null) {
            validateGoalNotBelowBalance(request.getGoalAmount(), pocket);
            pocket.setGoalAmount(request.getGoalAmount());
        }

        return savingsAccountMapper.toPocketResponse(pocketRepository.save(pocket));
    }

    // Regla de negocio: la meta de un bolsillo no puede ser menor a su saldo actual.
    // Aplica tanto en PATCH como en PUT para no dejar el bolsillo en un estado
    // inconsistente (meta ya superada) segun por que endpoint se edite.
    private void validateGoalNotBelowBalance(BigDecimal goalAmount, Pocket pocket) {
        if (goalAmount.compareTo(pocket.getBalance()) < 0) {
            throw new BusinessRuleException(
                    "Goal amount cannot be less than current balance: "
                            + pocket.getBalance());
        }
    }

    @Override
    @Transactional
    public void deletePocket(String userId, String accountId, String pocketId) {
        findAccountBelongingToUser(userId, accountId);

        Pocket pocket = pocketRepository.findByIdAndSavingsAccountId(pocketId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket", pocketId));

        pocket.setActive(false);
        pocketRepository.save(pocket);
    }

    @Override
    @Transactional
    public PocketResponse depositToPocket(String userId, String accountId,
                                          String pocketId, PocketAmountRequest request) {
        SavingsAccount account = findAccountBelongingToUser(userId, accountId);

        Pocket pocket = pocketRepository.findByIdAndSavingsAccountId(pocketId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket", pocketId));

        // Depositar mueve plata del disponible de la cuenta al bolsillo.
        // No se puede reservar más de lo que está libre. account.balance NO cambia.
        BigDecimal available = account.getAvailableBalance();
        if (request.getAmount().compareTo(available) > 0) {
            throw new InsufficientFundsException(
                    "Deposit exceeds available account balance: " + available);
        }

        pocket.setBalance(pocket.getBalance().add(request.getAmount()));

        return savingsAccountMapper.toPocketResponse(pocketRepository.save(pocket));
    }

    @Override
    @Transactional
    public PocketResponse withdrawFromPocket(String userId, String accountId,
                                             String pocketId, PocketAmountRequest request) {
        findAccountBelongingToUser(userId, accountId);

        Pocket pocket = pocketRepository.findByIdAndSavingsAccountId(pocketId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket", pocketId));

        // Retirar devuelve plata del bolsillo al disponible de la cuenta.
        // No se puede sacar más de lo que hay en el bolsillo. account.balance NO cambia.
        if (request.getAmount().compareTo(pocket.getBalance()) > 0) {
            throw new InsufficientFundsException(
                    "Withdrawal exceeds pocket balance: " + pocket.getBalance());
        }

        pocket.setBalance(pocket.getBalance().subtract(request.getAmount()));

        return savingsAccountMapper.toPocketResponse(pocketRepository.save(pocket));
    }

    private SavingsAccount findAccountBelongingToUser(String userId, String accountId) {
        return savingsAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount", accountId));
    }
}