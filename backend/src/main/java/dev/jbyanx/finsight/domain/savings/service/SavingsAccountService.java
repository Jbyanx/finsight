package dev.jbyanx.finsight.domain.savings.service;

import dev.jbyanx.finsight.domain.savings.dto.*;
import java.util.List;

public interface SavingsAccountService {

    SavingsAccountResponse create(String userId, SavingsAccountRequest request);

    SavingsAccountResponse getById(String userId, String accountId);

    List<SavingsAccountResponse> getAllByUser(String userId);

    SavingsAccountResponse update(String userId, String accountId, SavingsAccountRequest request);

    void delete(String userId, String accountId);

    // Bolsillos
    PocketResponse createPocket(String userId, String accountId, PocketRequest request);

    PocketResponse updatePocket(String userId, String accountId, String pocketId, PocketRequest request);

    void deletePocket(String userId, String accountId, String pocketId);
}