package dev.jbyanx.finsight.domain.income.service;

import dev.jbyanx.finsight.domain.income.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface IncomeService {

    // Fuentes de ingreso
    IncomeSourceResponse createSource(String userId, IncomeSourceRequest request);
    List<IncomeSourceResponse> getAllSources(String userId);
    IncomeSourceResponse updateSource(String userId, String sourceId, IncomeSourceRequest request);
    void deleteSource(String userId, String sourceId);

    // Registros de ingreso
    IncomeEntryResponse registerIncome(String userId, IncomeEntryRequest request);
    Page<IncomeEntryResponse> getIncomeHistory(String userId, Pageable pageable);

    // Gastos operativos
    OperationalExpenseResponse createExpense(String userId, OperationalExpenseRequest request);
    List<OperationalExpenseResponse> getAllExpenses(String userId);
    OperationalExpenseResponse updateExpense(String userId, String expenseId, OperationalExpenseRequest request);
    void deleteExpense(String userId, String expenseId);
}