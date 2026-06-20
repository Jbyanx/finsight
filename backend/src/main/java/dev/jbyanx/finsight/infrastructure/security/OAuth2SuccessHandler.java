package dev.jbyanx.finsight.infrastructure.security;

import dev.jbyanx.finsight.domain.auth.dto.AuthResponse;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        try {
            User user = userRepository.findByProviderAndProviderId("google", providerId)
                    .map(existingUser -> {
                        existingUser.setName(name);
                        existingUser.setPictureUrl(pictureUrl);
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> userRepository.findByEmail(email)
                            .map(localUser -> {
                                if (localUser.getPasswordHash() != null) {
                                    throw new UnauthorizedException(
                                            "An account with this email already exists. " +
                                                    "Please sign in with your password."
                                    );
                                }
                                localUser.setProvider("google");
                                localUser.setProviderId(providerId);
                                localUser.setName(name);
                                localUser.setPictureUrl(pictureUrl);
                                return userRepository.save(localUser);
                            })
                            .orElseGet(() -> userRepository.save(User.builder()
                                    .email(email)
                                    .name(name)
                                    .pictureUrl(pictureUrl)
                                    .provider("google")
                                    .providerId(providerId)
                                    .build())));

            String deviceInfo = request.getHeader("User-Agent");
            AuthResponse authResponse = authService.handleOAuth2Login(user, deviceInfo);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getWriter(), authResponse);

        } catch (UnauthorizedException ex) {
            // Manejo directo — estamos fuera del contexto del DispatcherServlet
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", ex.getMessage()
            ));
        }
    }
}