package io.showkart.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "roles", nullable = false)
    private String roles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {}

    UserJpaEntity(UUID userId, String email, String passwordHash, String roles, Instant createdAt) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    UUID getUserId() { return userId; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    String getRoles() { return roles; }
    Instant getCreatedAt() { return createdAt; }
}
