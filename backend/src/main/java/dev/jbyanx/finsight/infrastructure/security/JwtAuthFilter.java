package dev.jbyanx.finsight.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jbyanx.finsight.domain.auth.entity.Token;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.shared.exception.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper; // agrega este

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Si no hay token simplemente continúa — SecurityConfig decide si el endpoint es público
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = authHeader.substring(7);

        try {
            // Válida firma y expiración en el JWT
            if (!jwtService.isTokenValid(rawToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Verifica que el token existe en BD y no fue revocado
            Token token = tokenService.validateToken(rawToken);

            // Mapeamos la Entidad JPA al POJO de Seguridad.
            // Como estamos dentro del contexto de request (y validateToken probablemente
            // carga los datos o tiene sesión activa), esto extrae los datos reales, no el Proxy.
            UserPrincipal principal = UserPrincipal.create(token.getUser());

            // Auténtica en el contexto de Spring Security usando el PRINCIPAL, no la Entidad.
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (ExpiredJwtException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (MalformedJwtException | SignatureException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token invalid");
            return;
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(status, getErrorLabel(status), message, "")
        );
    }

    private String getErrorLabel(int status) {
        return switch (status) {
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            default -> "Error";
        };
    }
}