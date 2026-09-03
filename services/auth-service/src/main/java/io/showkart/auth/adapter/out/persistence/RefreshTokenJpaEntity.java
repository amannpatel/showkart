package io.showkart.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
class RefreshTokenJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    protected RefreshTokenJpaEntity() { }

    RefreshTokenJpaEntity(UUID id, UUID userId, String tokenHash,
                         Instant issuedAt, Instant expiresAt,
                         Instant revokedAt, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getTokenHash() { return tokenHash; }
    Instant getIssuedAt() { return issuedAt; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getRevokedAt() { return revokedAt; }
    void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    UUID getReplacedBy() { return replacedBy; }
    void setReplacedBy(UUID replacedBy) { this.replacedBy = replacedBy; }
}
