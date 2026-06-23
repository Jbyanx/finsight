package dev.jbyanx.finsight.domain.telegram.controller;

import dev.jbyanx.finsight.domain.telegram.dto.LinkCodeResponse;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramLinkController {

    private final TelegramLinkService telegramLinkService;

    @PostMapping("/link-code")
    public ResponseEntity<LinkCodeResponse> generateLinkCode(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                telegramLinkService.generateLinkCode(principal.getId()));
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<Void> unlink(
            @AuthenticationPrincipal UserPrincipal principal) {
        telegramLinkService.unlink(principal.getId());
        return ResponseEntity.noContent().build();
    }
}