package dev.jbyanx.finsight.domain.card.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.dto.CreditCardPatchRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.mapper.CreditCardMapper;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.shared.exception.InvalidCreditLimitException;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
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
    @Transactional
    public CreditCardResponse patch(String userId, String cardId,
                                    CreditCardPatchRequest request) {
        CreditCard card = findCardBelongingToUser(userId, cardId);

        if (request.getNickname() != null) card.setNickname(request.getNickname());
        if (request.getCreditLimit() != null) {
            validateCreditLimitNotBelowUsed(request.getCreditLimit(), card);
            card.setCreditLimit(request.getCreditLimit());
        }
        if (request.getCutOffDay() != null) card.setCutOffDay(request.getCutOffDay());
        if (request.getPaymentDueDay() != null) card.setPaymentDueDay(request.getPaymentDueDay());

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

        validateCreditLimitNotBelowUsed(request.getCreditLimit(), card);

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

    // Regla de negocio: el nuevo límite no puede ser menor al saldo utilizado.
    // Aplica tanto en PATCH como en PUT: bajar el cupo por debajo de lo usado
    // dejaría la tarjeta con utilización > 100%, un estado imposible.
    private void validateCreditLimitNotBelowUsed(BigDecimal newLimit, CreditCard card) {
        if (newLimit.compareTo(card.getUsedAmount()) < 0) {
            throw new InvalidCreditLimitException(newLimit, card.getUsedAmount());
        }
    }

    // Helper — verifica que la tarjeta existe Y pertenece al usuario
    private CreditCard findCardBelongingToUser(String userId, String cardId) {
        return creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditCard", cardId));
    }
}