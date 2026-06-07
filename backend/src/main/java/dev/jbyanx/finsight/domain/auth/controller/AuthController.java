package dev.jbyanx.finsight.domain.auth.controller;

import dev.jbyanx.finsight.domain.auth.dto.AuthResponse;
import dev.jbyanx.finsight.domain.auth.dto.SessionDTO;
import dev.jbyanx.finsight.domain.auth.dto.UserDTO;
import dev.jbyanx.finsight.domain.auth.mapper.TokenMapper;
import dev.jbyanx.finsight.domain.auth.mapper.UserMapper;
import dev.jbyanx.finsight.domain.auth.service.AuthService;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    // El login con Google no pasa por aquí
    // Spring Security maneja /oauth2/authorization/google directamente
    // Este endpoint solo existe para documentar el flujo al frontend
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loginInfo() {
        return ResponseEntity.ok(Map.of(
                "googleAuthUrl", "/oauth2/authorization/google",
                "message", "Redirect to googleAuthUrl to authenticate"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader(value = "User-Agent", defaultValue = "unknown") String deviceInfo) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken, deviceInfo));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllDevices(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logoutAllDevices(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(userMapper.toDTO(user));
    }

    @GetMapping("/sessions")
    public ResponseEntity<Page<SessionDTO>> getActiveSessions(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                tokenService.getActiveSessions(user.getId(), PageRequest.of(page, size))
        );
    }
}