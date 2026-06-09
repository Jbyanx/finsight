package dev.jbyanx.finsight.domain.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    @Email(message = "Email must be valid")
    private String email;

    @Column(length = 60) // BCrypt siempre genera 60 caracteres
    private String passwordHash; // null para usuarios OAuth2

    @Column(nullable = false)
    private String name;

    private String pictureUrl;

    @Column(nullable = false)
    private String provider; // "google"

    private String providerId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        lastLogin = LocalDateTime.now();
    }
}