package dev.jbyanx.finsight.domain.card.controller;

import dev.jbyanx.finsight.domain.card.dto.CreditCardRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;

    @PostMapping
    public ResponseEntity<CreditCardResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CreditCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditCardService.create(principal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<CreditCardResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(creditCardService.getAllByUser(principal.getId()));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CreditCardResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cardId) {
        return ResponseEntity.ok(creditCardService.getById(principal.getId(), cardId));
    }

    @GetMapping("/by-utilization")
    public ResponseEntity<List<CreditCardResponse>> getByUtilization(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                creditCardService.getOrderedByUtilization(principal.getId()));
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<CreditCardResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cardId,
            @RequestBody @Valid CreditCardRequest request) {
        return ResponseEntity.ok(
                creditCardService.update(principal.getId(), cardId, request));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String cardId) {
        creditCardService.delete(principal.getId(), cardId);
        return ResponseEntity.noContent().build();
    }
}