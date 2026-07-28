package dev.jbyanx.finsight.domain.card.service;

import dev.jbyanx.finsight.domain.card.dto.CardPaymentRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardPatchRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import java.util.List;

public interface CreditCardService {

    CreditCardResponse create(String userId, CreditCardRequest request);

    CreditCardResponse getById(String userId, String cardId);

    List<CreditCardResponse> getAllByUser(String userId);

    CreditCardResponse update(String userId, String cardId, CreditCardRequest request);

    void delete(String userId, String cardId);

    List<CreditCardResponse> getOrderedByUtilization(String userId);

    CreditCardResponse patch(String userId, String cardId, CreditCardPatchRequest request);

    CreditCardResponse applyPayment(String userId, String cardId, CardPaymentRequest request);
}