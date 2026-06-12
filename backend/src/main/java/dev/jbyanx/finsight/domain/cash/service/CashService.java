package dev.jbyanx.finsight.domain.cash.service;

import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.dto.CashUpdateRequest;

public interface CashService {
    CashResponse getByUser(String userId);
    CashResponse update(String userId, CashUpdateRequest request);
}