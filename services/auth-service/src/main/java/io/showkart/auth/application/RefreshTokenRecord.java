package io.showkart.auth.application;

import java.time.Instant;
import java.util.UUID;

/** Application-layer view of a refresh_token row, adapters map to/from this. */
public record RefreshTokenRecord(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        UUID replacedBy
) {
    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
