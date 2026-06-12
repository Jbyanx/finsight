package dev.jbyanx.finsight.domain.cash.controller;

import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.dto.CashUpdateRequest;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @GetMapping
    public ResponseEntity<CashResponse> get(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cashService.getByUser(principal.getId()));
    }

    @PatchMapping
    public ResponseEntity<CashResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CashUpdateRequest request) {
        return ResponseEntity.ok(cashService.update(principal.getId(), request));
    }
}