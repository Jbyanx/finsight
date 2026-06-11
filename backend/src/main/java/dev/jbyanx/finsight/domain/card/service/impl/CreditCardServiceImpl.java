package dev.jbyanx.finsight.domain.card.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.dto.CreditCardRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.mapper.CreditCardMapper;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditCardServiceImpl implements CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final CreditCardMapper creditCardMapper;

    @Override
    @Transactional
    public CreditCardResponse create(String userId, CreditCardRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        CreditCard card = CreditCard.builder()
                .user(user)
                .institution(request.getInstitution())
                .nickname(request.getNickname())
                .creditLimit(request.getCreditLimit())
                .cutOffDay(request.getCutOffDay())
                .paymentDueDay(request.getPaymentDueDay())
                .build();

        return creditCardMapper.toResponse(creditCardRepository.save(card));
    }

    @Override
    public CreditCardResponse getById(String userId, String cardId) {
        CreditCard card = findCardBelongingToUser(userId, cardId);
        return creditCardMapper.toResponse(card);
    }

    @Override
    public List<CreditCardResponse> getAllByUser(String userId) {
        return creditCardMapper.toResponseList(
                creditCardRepository.findAllByUserIdAndActiveTrue(userId)
        );
    }

    @Override
    @Transactional
    public CreditCardResponse update(String userId, String cardId, CreditCardRequest request) {
        CreditCard card = findCardBelongingToUser(userId, cardId);

        card.setInstitution(request.getInstitution());
        card.setNickname(request.getNickname());
        card.setCreditLimit(request.getCreditLimit());
        card.setCutOffDay(request.getCutOffDay());
        card.setPaymentDueDay(request.getPaymentDueDay());

        return creditCardMapper.toResponse(creditCardRepository.save(card));
    }

    @Override
    @Transactional
    public void delete(String userId, String cardId) {
        CreditCard card = findCardBelongingToUser(userId, cardId);
        card.setActive(false);
        creditCardRepository.save(card);
    }

    @Override
    public List<CreditCardResponse> getOrderedByUtilization(String userId) {
        return creditCardMapper.toResponseList(
                creditCardRepository.findAllByUserIdOrderByUtilizationAsc(userId)
        );
    }

    // Helper — verifica que la tarjeta existe Y pertenece al usuario
    private CreditCard findCardBelongingToUser(String userId, String cardId) {
        return creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditCard", cardId));
    }
}