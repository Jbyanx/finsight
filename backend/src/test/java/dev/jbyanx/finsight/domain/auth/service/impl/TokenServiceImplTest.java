package dev.jbyanx.finsight.domain.auth.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.Token;
import dev.jbyanx.finsight.domain.auth.entity.Token.TokenType;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.TokenRepository;
import dev.jbyanx.finsight.infrastructure.security.JwtService;
import dev.jbyanx.finsight.shared.exception.TokenExpiredException;
import dev.jbyanx.finsight.shared.exception.TokenInvalidException;
import dev.jbyanx.finsight.shared.exception.TokenRevokedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.jbyanx.finsight.domain.auth.mapper.TokenMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import dev.jbyanx.finsight.domain.auth.dto.SessionDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenMapper tokenMapper;

    @InjectMocks
    private TokenServiceImpl tokenService;

    // --- Fixtures reutilizables ---

    private User buildUser() {
        return User.builder()
                .id("user-123")
                .email("test@finsight.dev")
                .name("Test User")
                .provider("google")
                .active(true)
                .build();
    }

    private Token buildToken(TokenType type, boolean revoked, LocalDateTime expiresAt) {
        return Token.builder()
                .id("token-123")
                .token("raw-jwt-string")
                .type(type)
                .user(buildUser())
                .revoked(revoked)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- createAccessToken ---

    @Test
    void dadoUsuarioValido_cuandoCreateAccessToken_entoncesGuardaTokenEnBD() {
        // ARRANGE
        User user = buildUser();
        String rawJwt = "eyJhbGci...access";
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                .thenReturn(rawJwt);
        when(jwtService.extractExpiration(rawJwt))
                .thenReturn(LocalDateTime.now().plusMinutes(15));
        when(tokenRepository.save(any(Token.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        Token result = tokenService.createAccessToken(user, user.getId(), "Chrome/Linux");

        // ASSERT
        assertThat(result.getType()).isEqualTo(TokenType.ACCESS);
        assertThat(result.getToken()).isEqualTo(rawJwt);
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void dadoUsuarioValido_cuandoCreateRefreshToken_entoncesGuardaTokenEnBD() {
        User user = buildUser();
        String rawJwt = "eyJhbGci...refresh";
        when(jwtService.generateRefreshToken(anyString())).thenReturn(rawJwt);
        when(jwtService.extractExpiration(rawJwt))
                .thenReturn(LocalDateTime.now().plusDays(30));
        when(tokenRepository.save(any(Token.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Token result = tokenService.createRefreshToken(user, user.getId(), "Chrome/Linux");

        assertThat(result.getType()).isEqualTo(TokenType.REFRESH);
        verify(tokenRepository).save(any(Token.class));
    }

    // --- validateToken ---

    @Test
    void dadoTokenValido_cuandoValidateToken_entoncesRetornaToken() {
        Token token = buildToken(TokenType.ACCESS, false, LocalDateTime.now().plusMinutes(15));
        when(tokenRepository.findByToken("raw-jwt")).thenReturn(Optional.of(token));

        Token result = tokenService.validateToken("raw-jwt");

        assertThat(result.getId()).isEqualTo("token-123");
    }

    @Test
    void dadoTokenInexistente_cuandoValidateToken_entoncesLanzaTokenInvalidException() {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validateToken("token-falso"))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void dadoTokenRevocado_cuandoValidateToken_entoncesLanzaTokenRevokedException() {
        Token token = buildToken(TokenType.ACCESS, true, LocalDateTime.now().plusMinutes(15));
        when(tokenRepository.findByToken("raw-jwt")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.validateToken("raw-jwt"))
                .isInstanceOf(TokenRevokedException.class);
    }

    @Test
    void dadoTokenExpirado_cuandoValidateToken_entoncesLanzaTokenExpiredException() {
        Token token = buildToken(TokenType.ACCESS, false, LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("raw-jwt")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.validateToken("raw-jwt"))
                .isInstanceOf(TokenExpiredException.class);
    }

    // --- revokeToken ---

    @Test
    void dadoTokenExistente_cuandoRevokeToken_entoncesTokenQuedaRevocado() {
        Token token = buildToken(TokenType.ACCESS, false, LocalDateTime.now().plusMinutes(15));
        when(tokenRepository.findByToken("raw-jwt")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tokenService.revokeToken("raw-jwt");

        assertThat(token.isRevoked()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    void dadoTokenInexistente_cuandoRevokeToken_entoncesNoHaceNada() {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        tokenService.revokeToken("token-falso");

        verify(tokenRepository, never()).save(any());
    }

    // --- revokeAllUserTokens ---

    @Test
    void dadoUserId_cuandoRevokeAllUserTokens_entoncesLlamaAlRepositorio() {
        tokenService.revokeAllUserTokens("user-123");

        verify(tokenRepository).revokeAllUserTokensByUserId("user-123");
    }

    // --- getActiveSessions ---

    @Test
    void dadoUserId_cuandoGetActiveSessions_entoncesRetornaSessionDTOs() {
        Token token1 = buildToken(TokenType.ACCESS, false, LocalDateTime.now().plusMinutes(15));
        Token token2 = buildToken(TokenType.REFRESH, false, LocalDateTime.now().plusDays(30));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Token> page = new PageImpl<>(List.of(token1, token2));

        SessionDTO dto1 = SessionDTO.builder()
                .id("token-123")
                .type(TokenType.ACCESS.name())
                .deviceInfo("Chrome/Linux")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(tokenRepository.findAllByUserIdAndRevokedFalse("user-123", pageable))
                .thenReturn(page);
        when(tokenMapper.toSessionDTO(any(Token.class))).thenReturn(dto1);

        Page<SessionDTO> result = tokenService.getActiveSessions("user-123", pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).isEqualTo(TokenType.ACCESS.name());
        verify(tokenRepository).findAllByUserIdAndRevokedFalse("user-123", pageable);
        verify(tokenMapper, times(2)).toSessionDTO(any(Token.class));
    }
}