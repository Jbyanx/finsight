package dev.jbyanx.finsight.domain.auth.service.impl;

import dev.jbyanx.finsight.domain.auth.dto.AuthResponse;
import dev.jbyanx.finsight.domain.auth.dto.UserDTO;
import dev.jbyanx.finsight.domain.auth.entity.Token;
import dev.jbyanx.finsight.domain.auth.entity.Token.TokenType;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.infrastructure.security.JwtService;
import dev.jbyanx.finsight.shared.exception.TokenInvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    // --- Fixtures ---

    private User buildUser() {
        return User.builder()
                .id("user-123")
                .email("test@finsight.dev")
                .name("Test User")
                .pictureUrl("https://picture.url")
                .provider("google")
                .active(true)
                .build();
    }

    private Token buildToken(TokenType type) {
        return Token.builder()
                .id("token-123")
                .token("raw-jwt-" + type.name().toLowerCase())
                .type(type)
                .user(buildUser())
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- handleOAuth2Login ---

    @Test
    void dadoUsuarioValido_cuandoHandleOAuth2Login_entoncesRetornaAuthResponseCompleto() {
        // ARRANGE
        User user = buildUser();
        Token accessToken = buildToken(TokenType.ACCESS);
        Token refreshToken = buildToken(TokenType.REFRESH);

        when(tokenService.createAccessToken(eq(user), eq("user-123"), anyString()))
                .thenReturn(accessToken);
        when(tokenService.createRefreshToken(eq(user), eq("user-123"), anyString()))
                .thenReturn(refreshToken);

        // ACT
        AuthResponse response = authService.handleOAuth2Login(user, "Chrome/Linux");

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo(accessToken.getToken());
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getToken());
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
        assertThat(response.getUser().getEmail()).isEqualTo("test@finsight.dev");
    }

    @Test
    void dadoUsuarioValido_cuandoHandleOAuth2Login_entoncesCreaDosTokens() {
        User user = buildUser();
        when(tokenService.createAccessToken(any(), anyString(), anyString()))
                .thenReturn(buildToken(TokenType.ACCESS));
        when(tokenService.createRefreshToken(any(), anyString(), anyString()))
                .thenReturn(buildToken(TokenType.REFRESH));

        authService.handleOAuth2Login(user, "Chrome/Linux");

        verify(tokenService).createAccessToken(eq(user), eq("user-123"), eq("Chrome/Linux"));
        verify(tokenService).createRefreshToken(eq(user), eq("user-123"), eq("Chrome/Linux"));
    }

    // --- refreshToken ---

    @Test
    void dadoRefreshTokenValido_cuandoRefreshToken_entoncesRotaTokens() {
        // ARRANGE
        User user = buildUser();
        Token oldRefresh = buildToken(TokenType.REFRESH);
        Token newAccess = buildToken(TokenType.ACCESS);
        Token newRefresh = buildToken(TokenType.REFRESH);

        when(tokenService.validateToken("old-refresh")).thenReturn(oldRefresh);
        when(tokenService.createAccessToken(any(), anyString(), anyString()))
                .thenReturn(newAccess);
        when(tokenService.createRefreshToken(any(), anyString(), anyString()))
                .thenReturn(newRefresh);

        // ACT
        AuthResponse response = authService.refreshToken("old-refresh", "Chrome/Linux");

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo(newAccess.getToken());
        assertThat(response.getRefreshToken()).isEqualTo(newRefresh.getToken());
        // Verifica rotation — el token viejo fue revocado
        verify(tokenService).revokeToken("old-refresh");
    }

    @Test
    void dadoAccessTokenEnLugarDeRefresh_cuandoRefreshToken_entoncesLanzaTokenInvalidException() {
        // El access token no debe poder usarse para renovar
        Token accessToken = buildToken(TokenType.ACCESS);
        when(tokenService.validateToken("access-token")).thenReturn(accessToken);

        assertThatThrownBy(() -> authService.refreshToken("access-token", "Chrome/Linux"))
                .isInstanceOf(TokenInvalidException.class);

        // Verifica que NO se revocó ni se crearon tokens nuevos
        verify(tokenService, never()).revokeToken(anyString());
        verify(tokenService, never()).createAccessToken(any(), anyString(), anyString());
    }

    // --- logout ---

    @Test
    void dadoAccessTokenValido_cuandoLogout_entoncesRevocaToken() {
        authService.logout("access-token");

        verify(tokenService).revokeToken("access-token");
    }

    // --- logoutAllDevices ---

    @Test
    void dadoAccessTokenValido_cuandoLogoutAllDevices_entoncesRevocaTodosLosTokensDelUsuario() {
        Token token = buildToken(TokenType.ACCESS);
        when(tokenService.validateToken("access-token")).thenReturn(token);

        authService.logoutAllDevices("access-token");

        verify(tokenService).revokeAllUserTokens("user-123");
    }

    // --- getCurrentUser ---

    @Test
    void dadoAccessTokenValido_cuandoGetCurrentUser_entoncesRetornaUserDTO() {
        Token token = buildToken(TokenType.ACCESS);
        when(tokenService.validateToken("access-token")).thenReturn(token);

        UserDTO result = authService.getCurrentUser("access-token");

        assertThat(result.getId()).isEqualTo("user-123");
        assertThat(result.getEmail()).isEqualTo("test@finsight.dev");
        assertThat(result.getProvider()).isEqualTo("google");
    }

    @Test
    void dadoTokenInvalido_cuandoGetCurrentUser_entoncesLanzaExcepcion() {
        when(tokenService.validateToken(anyString()))
                .thenThrow(new TokenInvalidException());

        assertThatThrownBy(() -> authService.getCurrentUser("token-invalido"))
                .isInstanceOf(TokenInvalidException.class);
    }
}