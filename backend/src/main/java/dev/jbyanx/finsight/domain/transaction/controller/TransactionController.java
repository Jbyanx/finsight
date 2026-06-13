package dev.jbyanx.finsight.domain.transaction.controller;

import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionResponse;
import dev.jbyanx.finsight.domain.transaction.service.TransactionService;
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

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(principal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getAll(
                principal.getId(),
                PageRequest.of(page, size, Sort.by("date").descending())));
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<Page<TransactionResponse>> getByCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getByCard(
                principal.getId(), cardId,
                PageRequest.of(page, size, Sort.by("date").descending())));
    }

    @GetMapping("/shared")
    public ResponseEntity<Page<TransactionResponse>> getShared(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getShared(
                principal.getId(),
                PageRequest.of(page, size, Sort.by("date").descending())));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String transactionId) {
        return ResponseEntity.ok(
                transactionService.getById(principal.getId(), transactionId));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String transactionId) {
        transactionService.delete(principal.getId(), transactionId);
        return ResponseEntity.noContent().build();
    }
}