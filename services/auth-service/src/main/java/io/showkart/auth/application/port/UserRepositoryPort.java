package io.showkart.auth.application.port;

import io.showkart.auth.domain.UserAccount;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    UserAccount save(UserAccount user);
}
