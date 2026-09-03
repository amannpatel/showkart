package io.showkart.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String passwordHash,
        String roles,
        Instant createdAt
) {
    public static final String DEFAULT_ROLE = "ROLE_USER";
}
