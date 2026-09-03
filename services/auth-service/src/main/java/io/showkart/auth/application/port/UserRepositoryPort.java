package io.showkart.auth.application.port;

import io.showkart.auth.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findById(UUID userId);

    UserAccount save(UserAccount user);
}
