package dev.jbyanx.finsight.domain.auth.service.impl;

import dev.jbyanx.finsight.domain.auth.dto.SessionDTO;
import dev.jbyanx.finsight.domain.auth.entity.Token;
import dev.jbyanx.finsight.domain.auth.entity.Token.TokenType;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.mapper.TokenMapper;
import dev.jbyanx.finsight.domain.auth.repository.TokenRepository;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final TokenMapper tokenMapper;

    @Override
    @Transactional
    public Token createAccessToken(User user, String userId, String deviceInfo) {
        String rawToken = jwtService.generateAccessToken(userId, user.getEmail(), user.getName());
        return tokenRepository.save(Token.builder()
                .token(rawToken)
                .type(TokenType.ACCESS)
                .user(user)
                .deviceInfo(deviceInfo)
                .expiresAt(jwtService.extractExpiration(rawToken))
                .build());
    }

    @Override
    @Transactional
    public Token createRefreshToken(User user, String userId, String deviceInfo) {
        String rawToken = jwtService.generateRefreshToken(userId);
        return tokenRepository.save(Token.builder()
                .token(rawToken)
                .type(TokenType.REFRESH)
                .user(user)
                .deviceInfo(deviceInfo)
                .expiresAt(jwtService.extractExpiration(rawToken))
                .build());
    }

    @Override
    public Token validateToken(String rawToken) {
        return tokenRepository.findByToken(rawToken)
                .filter(Token::isValid)
                .orElseThrow(() -> new RuntimeException("Token invalid or expired"));
    }

    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        tokenRepository.findByToken(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(String userId) {
        tokenRepository.revokeAllUserTokensByUserId(userId);
    }

    // TokenServiceImpl
    @Override
    public Page<SessionDTO> getActiveSessions(String userId, Pageable pageable) {
        return tokenRepository.findAllByUserIdAndRevokedFalse(userId, pageable)
                .map(tokenMapper::toSessionDTO);
    }
}