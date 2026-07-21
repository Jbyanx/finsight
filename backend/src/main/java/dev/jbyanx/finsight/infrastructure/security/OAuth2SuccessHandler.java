package dev.jbyanx.finsight.infrastructure.security;

import dev.jbyanx.finsight.domain.auth.dto.AuthResponse;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.AuthService;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cierra el login OAuth2 de Google y devuelve el control al FRONT.
 *
 * POR QUÉ REDIRIGE Y NO ESCRIBE JSON:
 * El login OAuth es una NAVEGACIÓN del navegador, no una llamada AJAX. El flujo
 * es: el usuario navega a /oauth2/authorization/google, va a Google, y Google lo
 * devuelve a /login/oauth2/code/google. Si acá escribiéramos JSON en el body, el
 * usuario terminaría viendo una página con sus tokens en texto plano, y el SPA
 * nunca los recibiría (no hay ningún fetch esperando esa respuesta).
 *
 * Por eso redirigimos al front con los tokens en el FRAGMENTO (#) de la URL.
 * El fragmento nunca se envía al servidor: no aparece en logs de acceso, ni en
 * el header Referer, ni en proxies. El front lo lee, lo guarda y lo borra de la
 * barra de direcciones con history.replaceState.
 *
 * (Un query param —?token=...— sí viajaría al servidor y quedaría registrado en
 * los logs de cualquier intermediario. De ahí la diferencia.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
            User user = resolveUser(email, name, pictureUrl, providerId);

            String deviceInfo = request.getHeader("User-Agent");
            AuthResponse auth = authService.handleOAuth2Login(user, deviceInfo);

            response.sendRedirect(successUrl(auth));

        } catch (UnauthorizedException ex) {
            // El usuario está en el navegador, no en Postman: también hay que
            // devolverlo al front, con el motivo del fallo.
            log.warn("OAuth2 login rejected for {}: {}", email, ex.getMessage());
            response.sendRedirect(failureUrl(ex.getMessage()));
        }
    }

    /**
     * Login inteligente: si ya existe por provider, actualiza; si existe por
     * email pero es una cuenta local con contraseña, no permite el secuestro;
     * si no existe, lo crea.
     */
    private User resolveUser(String email, String name, String pictureUrl, String providerId) {
        return userRepository.findByProviderAndProviderId("google", providerId)
                .map(existingUser -> {
                    existingUser.setName(name);
                    existingUser.setPictureUrl(pictureUrl);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(localUser -> {
                            if (localUser.getPasswordHash() != null) {
                                throw new UnauthorizedException(
                                        "Ya existe una cuenta con este correo. "
                                                + "Inicia sesión con tu contraseña.");
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
    }

    /** Tokens en el fragmento: el navegador los ve, el servidor no. */
    private String successUrl(AuthResponse auth) {
        String fragment = "accessToken=" + auth.getAccessToken()
                + "&refreshToken=" + auth.getRefreshToken()
                + "&expiresIn=" + auth.getExpiresIn();

        return UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/auth/callback")
                .fragment(fragment)
                .build()
                .toUriString();
    }

    private String failureUrl(String message) {
        return UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/login")
                .queryParam("error", URLEncoder.encode(message, StandardCharsets.UTF_8))
                .build()
                .toUriString();
    }
}