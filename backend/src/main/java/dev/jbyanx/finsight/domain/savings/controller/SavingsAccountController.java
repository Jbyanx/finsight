package dev.jbyanx.finsight.domain.savings.controller;

import dev.jbyanx.finsight.domain.savings.dto.*;
import dev.jbyanx.finsight.domain.savings.service.SavingsAccountService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/savings")
@RequiredArgsConstructor
public class SavingsAccountController {

    private final SavingsAccountService savingsAccountService;

    @PostMapping
    public ResponseEntity<SavingsAccountResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid SavingsAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsAccountService.create(principal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<SavingsAccountResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(savingsAccountService.getAllByUser(principal.getId()));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<SavingsAccountResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId) {
        return ResponseEntity.ok(savingsAccountService.getById(principal.getId(), accountId));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<SavingsAccountResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId,
            @RequestBody @Valid SavingsAccountRequest request) {
        return ResponseEntity.ok(
                savingsAccountService.update(principal.getId(), accountId, request));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<SavingsAccountResponse> patch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId,
            @RequestBody @Valid SavingsAccountPatchRequest request) {
        return ResponseEntity.ok(
                savingsAccountService.patch(principal.getId(), accountId, request));
    }

    @PatchMapping("/{accountId}/pockets/{pocketId}")
    public ResponseEntity<PocketResponse> patchPocket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId,
            @PathVariable String pocketId,
            @RequestBody @Valid PocketPatchRequest request) {
        return ResponseEntity.ok(savingsAccountService.patchPocket(
                principal.getId(), accountId, pocketId, request));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId) {
        savingsAccountService.delete(principal.getId(), accountId);
        return ResponseEntity.noContent().build();
    }

    // --- Bolsillos ---

    @PostMapping("/{accountId}/pockets")
    public ResponseEntity<PocketResponse> createPocket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId,
            @RequestBody @Valid PocketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsAccountService.createPocket(
                        principal.getId(), accountId, request));
    }

    @PutMapping("/{accountId}/pockets/{pocketId}")
    public ResponseEntity<PocketResponse> updatePocket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId,
            @PathVariable String pocketId,
            @RequestBody @Valid PocketRequest request) {
        return ResponseEntity.ok(savingsAccountService.updatePocket(
                principal.getId(), accountId, pocketId, request));
    }

    @DeleteMapping("/{accountId}/pockets/{pocketId}")
    public ResponseEntity<Void> deletePocket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String accountId,
            @PathVariable String pocketId) {
        savingsAccountService.deletePocket(principal.getId(), accountId, pocketId);
        return ResponseEntity.noContent().build();
    }
}