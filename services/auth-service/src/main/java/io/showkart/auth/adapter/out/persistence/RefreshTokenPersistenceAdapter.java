package io.showkart.auth.adapter.out.persistence;

import io.showkart.auth.application.RefreshTokenRecord;
import io.showkart.auth.application.port.RefreshTokenRepositoryPort;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Repository
class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;
    private final Clock clock;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public RefreshTokenRecord save(RefreshTokenRecord record) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(
                record.id(),
                record.userId(),
                record.tokenHash(),
                record.issuedAt(),
                record.expiresAt(),
                record.revokedAt(),
                record.replacedBy()
        );
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public void revoke(UUID id, UUID replacementId) {
        repository.revokeIfActive(id, clock.instant(), replacementId);
    }

    private static RefreshTokenRecord toDomain(RefreshTokenJpaEntity e) {
        return new RefreshTokenRecord(
                e.getId(), e.getUserId(), e.getTokenHash(),
                e.getIssuedAt(), e.getExpiresAt(), e.getRevokedAt(), e.getReplacedBy());
    }
}
