package dev.jbyanx.finsight.domain.auth.repository;

import dev.jbyanx.finsight.domain.auth.entity.Token;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {
    @EntityGraph(attributePaths = {"user"})
    Optional<Token> findByToken(String token);

    // Todos los tokens activos de un usuario — para ver sesiones activas
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT t FROM Token t WHERE t.user.id = :userId AND t.revoked = false")
    Page<Token> findAllByUserIdAndRevokedFalse(String userId, Pageable pageable);

    // Revocar todos los tokens de un usuario — logout de todas las sesiones
    @Modifying
    @Query("UPDATE Token t SET t.revoked = true WHERE t.user.id = :userId")
    void revokeAllUserTokensByUserId(String userId);

    // Para el @Scheduled — borrar tokens expirados o revocados
    @Modifying
    @Query("DELETE FROM Token t WHERE t.revoked = true OR t.expiresAt < :now")
    void deleteExpiredOrRevokedTokens(LocalDateTime now);

}