package io.showkart.auth.application.port;

import io.showkart.auth.application.RefreshTokenRecord;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshTokenRecord save(RefreshTokenRecord record);

    Optional<RefreshTokenRecord> findByHash(String tokenHash);

    /** Sets revoked_at = now and replaced_by = replacement. */
    void revoke(UUID id, UUID replacementId);
}
