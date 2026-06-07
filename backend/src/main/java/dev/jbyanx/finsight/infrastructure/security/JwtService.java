package dev.jbyanx.finsight.infrastructure.security;

import dev.jbyanx.finsight.domain.auth.entity.Token.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Genera un access token con datos primitivos.
     * @param userId El ID del usuario (será el subject del JWT)
     * @param email Email del usuario (valor adicional en claims)
     * @param name Nombre del usuario (valor adicional en claims)
     */
    public String generateAccessToken(String userId, String email, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TokenType.ACCESS.name());
        claims.put("email", email);
        claims.put("name", name);
        return buildToken(claims, userId, accessExpirationMs);
    }

    /**
     * Genera un refresh token con datos primitivos.
     * @param userId El ID del usuario (será el subject del JWT)
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TokenType.REFRESH.name());
        return buildToken(claims, userId, refreshExpirationMs);
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public LocalDateTime extractExpiration(String token) {
        Date expiration = extractClaims(token).getExpiration();
        return expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}