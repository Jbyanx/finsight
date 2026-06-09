package dev.jbyanx.finsight.infrastructure.security;

import dev.jbyanx.finsight.domain.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Representa el usuario autenticado en el contexto de seguridad de Spring.
 * Evita guardar entidades JPA directamente para prevenir LazyInitializationException.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String name;
    private final String pictureUrl;
    private final String provider;
    private final boolean active;
    private final String passwordHash;

    // Si en el futuro agregas roles, aquí irían. Por ahora, lista vacía.
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Factory method para mapear la entidad JPA a este objeto inmutable en memoria.
     */

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getProvider(),
                user.isActive(),
                user.getPasswordHash(),
                Collections.emptyList()
        );
    }

    @Override
    public String getPassword() {
        return this.passwordHash; // ya no es null
    }

    // --- Métodos obligatorios de UserDetails de Spring Security ---

    @Override
    public String getUsername() {
        return this.email; // Spring Security usa "username", para nosotros es el email.
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active; // Mapeamos el estado "enabled" al campo active de tu DB.
    }
}