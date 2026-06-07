package dev.jbyanx.finsight.domain.auth.service;

import dev.jbyanx.finsight.domain.auth.dto.SessionDTO;
import dev.jbyanx.finsight.domain.auth.entity.Token;
import dev.jbyanx.finsight.domain.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TokenService {

    /**
     * Crea un access token para un usuario.
     * @param user Entidad User cargada del contexto transaccional (será asignada al token)
     * @param userId ID del usuario (para generar el JWT)
     * @param deviceInfo Información del dispositivo para auditoría
     */
    Token createAccessToken(User user, String userId, String deviceInfo);

    /**
     * Crea un refresh token para un usuario.
     * @param user Entidad User cargada del contexto transaccional (será asignada al token)
     * @param userId ID del usuario (para generar el JWT)
     * @param deviceInfo Información del dispositivo para auditoría
     */
    Token createRefreshToken(User user, String userId, String deviceInfo);

    Token validateToken(String rawToken);

    void revokeToken(String rawToken);

    /**
     * Revoca todos los tokens activos de un usuario (logout desde todos los dispositivos)
     * @param userId ID del usuario
     */
    void revokeAllUserTokens(String userId);

    /**
     * Obtiene las sesiones activas de un usuario
     * @param userId ID del usuario
     */
    Page<SessionDTO> getActiveSessions(String userId, Pageable pageable);
}