package dev.jbyanx.finsight.domain.auth.service.impl;

import dev.jbyanx.finsight.domain.auth.dto.AuthResponse;
import dev.jbyanx.finsight.domain.auth.dto.LoginRequest;
import dev.jbyanx.finsight.domain.auth.dto.RegisterRequest;
import dev.jbyanx.finsight.domain.auth.dto.UserDTO;
import dev.jbyanx.finsight.domain.auth.entity.Token;
import dev.jbyanx.finsight.domain.auth.entity.Token.TokenType;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.AuthService;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.infrastructure.security.JwtService;
import dev.jbyanx.finsight.shared.exception.TokenInvalidException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import dev.jbyanx.finsight.shared.exception.UserAlreadyExistsException;
import dev.jbyanx.finsight.shared.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String deviceInfo) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("local")
                .build();

        userRepository.save(user);

        Token accessToken = tokenService.createAccessToken(user, user.getId(), deviceInfo);
        Token refreshToken = tokenService.createRefreshToken(user, user.getId(), deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .user(toDTO(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // Usuario registrado con OAuth2 intenta hacer login con contraseña
        if ("google".equals(user.getProvider())) {
            throw new UnauthorizedException(
                    "This account uses Google login. Please sign in with Google."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        Token accessToken = tokenService.createAccessToken(user, user.getId(), deviceInfo);
        Token refreshToken = tokenService.createRefreshToken(user, user.getId(), deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .user(toDTO(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse handleOAuth2Login(User user, String deviceInfo) {
        Token accessToken = tokenService.createAccessToken(user, user.getId(), deviceInfo);
        Token refreshToken = tokenService.createRefreshToken(user, user.getId(), deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .user(toDTO(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken, String deviceInfo) {
        Token refreshToken = tokenService.validateToken(rawRefreshToken);

        if (refreshToken.getType() != TokenType.REFRESH) {
            throw new TokenInvalidException();
        }

        User user = refreshToken.getUser();

        // Revocar el refresh token usado — rotation strategy
        tokenService.revokeToken(rawRefreshToken);

        // Generar nuevos tokens
        Token newAccessToken = tokenService.createAccessToken(user, user.getId(), deviceInfo);
        Token newRefreshToken = tokenService.createRefreshToken(user, user.getId(), deviceInfo);

        return AuthResponse.builder()
                .accessToken(newAccessToken.getToken())
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .user(toDTO(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        tokenService.revokeToken(accessToken);
    }

    @Override
    @Transactional
    public void logoutAllDevices(String accessToken) {
        Token token = tokenService.validateToken(accessToken);
        String userId = token.getUser().getId();
        tokenService.revokeAllUserTokens(userId);
    }

    @Override
    public UserDTO getCurrentUser(String accessToken) {
        Token token = tokenService.validateToken(accessToken);
        return toDTO(token.getUser());
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .pictureUrl(user.getPictureUrl())
                .provider(user.getProvider())
                .build();
    }
}