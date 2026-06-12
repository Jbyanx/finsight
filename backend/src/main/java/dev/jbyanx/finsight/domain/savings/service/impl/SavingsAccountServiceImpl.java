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
                .isShared(request.isShared())
                .sharedOwnerName(request.getSharedOwnerName())
                .build();

        return savingsAccountMapper.toResponse(savingsAccountRepository.save(account));
    }

    @Override
    public SavingsAccountResponse getById(String userId, String accountId) {
        return savingsAccountMapper.toResponse(findAccountBelongingToUser(userId, accountId));
    }

    @Override
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

        Pocket pocket = Pocket.builder()
                .savingsAccount(account)
                .name(request.getName())
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
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
        if (request.getGoalAmount() != null) pocket.setGoalAmount(request.getGoalAmount());

        return savingsAccountMapper.toPocketResponse(pocketRepository.save(pocket));
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

    private SavingsAccount findAccountBelongingToUser(String userId, String accountId) {
        return savingsAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount", accountId));
    }
}