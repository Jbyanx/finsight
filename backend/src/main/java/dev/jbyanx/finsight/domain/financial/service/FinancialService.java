package dev.jbyanx.finsight.domain.financial.service;

import dev.jbyanx.finsight.domain.financial.dto.FinancialSummaryResponse;

public interface FinancialService {
    FinancialSummaryResponse getSummary(String userId);
}