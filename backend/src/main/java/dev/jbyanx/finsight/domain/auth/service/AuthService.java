package dev.jbyanx.finsight.domain.auth.service;

import dev.jbyanx.finsight.domain.auth.dto.AuthResponse;
import dev.jbyanx.finsight.domain.auth.dto.LoginRequest;
import dev.jbyanx.finsight.domain.auth.dto.RegisterRequest;
import dev.jbyanx.finsight.domain.auth.dto.UserDTO;
import dev.jbyanx.finsight.domain.auth.entity.User;

public interface AuthService {

    AuthResponse handleOAuth2Login(User user, String deviceInfo);

    AuthResponse refreshToken(String refreshToken, String deviceInfo);

    void logout(String accessToken);

    void logoutAllDevices(String accessToken);

    UserDTO getCurrentUser(String accessToken);

    AuthResponse register(RegisterRequest request, String deviceInfo);
    AuthResponse login(LoginRequest request, String deviceInfo);
}