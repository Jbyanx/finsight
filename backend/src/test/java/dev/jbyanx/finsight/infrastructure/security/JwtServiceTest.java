package dev.jbyanx.finsight.infrastructure.security;

import dev.jbyanx.finsight.domain.auth.entity.Token.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    // Datos de prueba
    private static final String USER_ID = "test-user-id-123";
    private static final String EMAIL = "test@finsight.dev";
    private static final String NAME = "Test User";
    private static final String SECRET =
            "test-secret-key-minimum-256-bits-long-enough-for-hmac-sha";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inyecta los @Value manualmente sin contexto de Spring
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 2592000000L);
    }

    @Test
    void dadoUsuarioValido_cuandoGenerateAccessToken_entoncesTokenContieneUserId() {
        // ACT
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, NAME);

        // ASSERT
        assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    void dadoUsuarioValido_cuandoGenerateAccessToken_entoncesTokenTipoAccess() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, NAME);

        assertThat(jwtService.extractTokenType(token)).isEqualTo(TokenType.ACCESS.name());
    }

    @Test
    void dadoUsuarioValido_cuandoGenerateRefreshToken_entoncesTokenTipoRefresh() {
        String token = jwtService.generateRefreshToken(USER_ID);

        assertThat(jwtService.extractTokenType(token)).isEqualTo(TokenType.REFRESH.name());
    }

    @Test
    void dadoTokenValido_cuandoIsTokenValid_entoncesRetornaTrue() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, NAME);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void dadoTokenMalformado_cuandoIsTokenValid_entoncesRetornaFalse() {
        assertThat(jwtService.isTokenValid("esto.no.es.un.jwt")).isFalse();
    }

    @Test
    void dadoTokenVacio_cuandoIsTokenValid_entoncesRetornaFalse() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void dadoTokenExpirado_cuandoIsTokenValid_entoncesRetornaFalse() {
        // Token con expiración en el pasado
        JwtService expiredService = new JwtService();
        ReflectionTestUtils.setField(expiredService, "secret", SECRET);
        ReflectionTestUtils.setField(expiredService, "accessExpirationMs", -1000L);
        ReflectionTestUtils.setField(expiredService, "refreshExpirationMs", -1000L);

        String expiredToken = expiredService.generateAccessToken(USER_ID, EMAIL, NAME);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void dadoAccessToken_cuandoExtractExpiration_entoncesRetornaFechaFutura() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, NAME);

        assertThat(jwtService.extractExpiration(token))
                .isAfter(java.time.LocalDateTime.now());
    }
}