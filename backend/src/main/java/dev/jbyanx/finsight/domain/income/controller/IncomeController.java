package dev.jbyanx.finsight.domain.income.controller;

import dev.jbyanx.finsight.domain.income.dto.*;
import dev.jbyanx.finsight.domain.income.service.IncomeService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/income")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    // --- Fuentes de ingreso ---

    @PostMapping("/sources")
    public ResponseEntity<IncomeSourceResponse> createSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid IncomeSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.createSource(principal.getId(), request));
    }

    @GetMapping("/sources")
    public ResponseEntity<List<IncomeSourceResponse>> getAllSources(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incomeService.getAllSources(principal.getId()));
    }

    @PutMapping("/sources/{sourceId}")
    public ResponseEntity<IncomeSourceResponse> updateSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sourceId,
            @RequestBody @Valid IncomeSourceRequest request) {
        return ResponseEntity.ok(
                incomeService.updateSource(principal.getId(), sourceId, request));
    }

    @DeleteMapping("/sources/{sourceId}")
    public ResponseEntity<Void> deleteSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sourceId) {
        incomeService.deleteSource(principal.getId(), sourceId);
        return ResponseEntity.noContent().build();
    }

    // --- Registros de ingreso ---

    @PostMapping("/entries")
    public ResponseEntity<IncomeEntryResponse> registerIncome(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid IncomeEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.registerIncome(principal.getId(), request));
    }

    @GetMapping("/entries")
    public ResponseEntity<Page<IncomeEntryResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(incomeService.getIncomeHistory(
                principal.getId(),
                PageRequest.of(page, size, Sort.by("receivedAt").descending())));
    }

    // --- Gastos operativos ---

    @PostMapping("/expenses")
    public ResponseEntity<OperationalExpenseResponse> createExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid OperationalExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.createExpense(principal.getId(), request));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<OperationalExpenseResponse>> getAllExpenses(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incomeService.getAllExpenses(principal.getId()));
    }

    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<OperationalExpenseResponse> updateExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String expenseId,
            @RequestBody @Valid OperationalExpenseRequest request) {
        return ResponseEntity.ok(
                incomeService.updateExpense(principal.getId(), expenseId, request));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String expenseId) {
        incomeService.deleteExpense(principal.getId(), expenseId);
        return ResponseEntity.noContent().build();
    }
}