package dev.jbyanx.finsight.domain.financial.controller;

import dev.jbyanx.finsight.domain.financial.dto.FinancialSummaryResponse;
import dev.jbyanx.finsight.domain.financial.service.FinancialService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @GetMapping("/summary")
    public ResponseEntity<FinancialSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(financialService.getSummary(principal.getId()));
    }
}