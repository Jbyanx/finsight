package dev.jbyanx.finsight.domain.cash.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.cash.dto.CashOperation;
import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.dto.CashUpdateRequest;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.mapper.CashMapper;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final CashRepository cashRepository;
    private final UserRepository userRepository;
    private final CashMapper cashMapper;

    @Override
    public CashResponse getByUser(String userId) {
        Cash cash = getOrCreateCash(userId);
        return cashMapper.toResponse(cash);
    }

    @Override
    @Transactional
    public CashResponse update(String userId, CashUpdateRequest request) {
        Cash cash = getOrCreateCash(userId);

        if (request.getOperation() == CashOperation.ADD) {
            cash.add(request.getAmount());
        } else {
            cash.subtract(request.getAmount());
        }

        return cashMapper.toResponse(cashRepository.save(cash));
    }

    private Cash getOrCreateCash(String userId) {
        return cashRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    return cashRepository.save(Cash.builder()
                            .user(user)
                            .build());
                });
    }
}