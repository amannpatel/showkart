package io.showkart.auth.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    /** Row-level lock so two concurrent /refresh calls serialize on the same token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    /** Atomic revoke: single UPDATE, no read-modify-write race. */
    @Modifying
    @Query("update RefreshTokenJpaEntity r set r.revokedAt = :revokedAt, r.replacedBy = :replacement " +
           "where r.id = :id and r.revokedAt is null")
    int revokeIfActive(@Param("id") UUID id,
                       @Param("revokedAt") Instant revokedAt,
                       @Param("replacement") UUID replacement);
}
