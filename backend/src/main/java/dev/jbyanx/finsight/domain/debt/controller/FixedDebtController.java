package dev.jbyanx.finsight.domain.debt.controller;

import dev.jbyanx.finsight.domain.debt.dto.*;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/debts")
@RequiredArgsConstructor
public class FixedDebtController {

    private final FixedDebtService fixedDebtService;

    @PostMapping
    public ResponseEntity<FixedDebtResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid FixedDebtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fixedDebtService.create(principal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<FixedDebtResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fixedDebtService.getAllByUser(principal.getId()));
    }

    @GetMapping("/{debtId}")
    public ResponseEntity<FixedDebtResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String debtId) {
        return ResponseEntity.ok(
                fixedDebtService.getById(principal.getId(), debtId));
    }

    @PatchMapping("/{debtId}")
    public ResponseEntity<FixedDebtResponse> patch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String debtId,
            @RequestBody @Valid FixedDebtPatchRequest request) {
        return ResponseEntity.ok(
                fixedDebtService.patch(principal.getId(), debtId, request));
    }

    @DeleteMapping("/{debtId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String debtId) {
        fixedDebtService.delete(principal.getId(), debtId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{debtId}/payments")
    public ResponseEntity<FixedDebtResponse> applyPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String debtId,
            @RequestBody @Valid DebtPaymentRequest request) {
        return ResponseEntity.ok(
                fixedDebtService.applyPayment(principal.getId(), debtId, request));
    }
}