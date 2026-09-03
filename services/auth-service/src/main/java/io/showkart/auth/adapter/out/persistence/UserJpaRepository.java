package io.showkart.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    // citext column already normalizes case; Spring Data infers WHERE email = ?
    Optional<UserJpaEntity> findByEmail(String email);
}
