package dev.jbyanx.finsight.domain.transaction.service;

import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    TransactionResponse create(String userId, TransactionRequest request);

    Page<TransactionResponse> getAll(String userId, Pageable pageable);

    Page<TransactionResponse> getByCard(String userId, String cardId, Pageable pageable);

    Page<TransactionResponse> getShared(String userId, Pageable pageable);

    TransactionResponse getById(String userId, String transactionId);

    void delete(String userId, String transactionId);
}