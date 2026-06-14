package dev.jbyanx.finsight.domain.debt.service;

import dev.jbyanx.finsight.domain.debt.dto.*;
import java.util.List;

public interface FixedDebtService {

    FixedDebtResponse create(String userId, FixedDebtRequest request);

    List<FixedDebtResponse> getAllByUser(String userId);

    FixedDebtResponse getById(String userId, String debtId);

    FixedDebtResponse patch(String userId, String debtId, FixedDebtPatchRequest request);

    void delete(String userId, String debtId);

    FixedDebtResponse applyPayment(String userId, String debtId,
                                   DebtPaymentRequest request);
}