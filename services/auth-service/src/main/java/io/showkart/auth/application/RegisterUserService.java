package io.showkart.auth.application;

import io.showkart.auth.application.port.PasswordEncoderPort;
import io.showkart.auth.application.port.UserRepositoryPort;
import io.showkart.auth.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    // NFR-SEC per spec §Boundaries; keep in sync with WeakPasswordException message.
    static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt silently truncates inputs > 72 bytes, which would let two different long
    // passwords sharing a 72-byte prefix authenticate identically — reject up front.
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwords;

    public RegisterUserService(UserRepositoryPort users, PasswordEncoderPort passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public Result register(Command command) {
        String email = normalizeEmail(command.email());
        validatePassword(command.password());

        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new EmailTakenException(email);
        });

        UserAccount fresh = new UserAccount(
                UUID.randomUUID(),
                email,
                passwords.encode(command.password()),
                UserAccount.DEFAULT_ROLE,
                Instant.now()
        );
        return new Result(users.save(fresh));
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    private static void validatePassword(String raw) {
        if (raw == null || raw.isBlank() || raw.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters."
            );
        }
        if (raw.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new WeakPasswordException(
                    "Password must not exceed " + MAX_PASSWORD_BYTES + " bytes (UTF-8)."
            );
        }
    }
}
