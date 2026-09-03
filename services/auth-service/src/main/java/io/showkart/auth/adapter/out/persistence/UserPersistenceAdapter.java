package io.showkart.auth.adapter.out.persistence;

import io.showkart.auth.application.port.UserRepositoryPort;
import io.showkart.auth.domain.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByEmailIgnoreCase(String email) {
        return repository.findByEmail(email).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return repository.findById(userId).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public UserAccount save(UserAccount user) {
        UserJpaEntity persisted = repository.save(toEntity(user));
        return toDomain(persisted);
    }

    private static UserAccount toDomain(UserJpaEntity entity) {
        return new UserAccount(
                entity.getUserId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRoles(),
                entity.getCreatedAt()
        );
    }

    private static UserJpaEntity toEntity(UserAccount user) {
        return new UserJpaEntity(
                user.id(),
                user.email(),
                user.passwordHash(),
                user.roles(),
                user.createdAt()
        );
    }
}
