package io.showkart.auth.application.port;

import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;

import java.util.UUID;

public interface TokenIssuerPort {

    AccessToken issueAccessToken(UUID userId, String roles);

    RefreshTokenValue issueRefreshToken();

    /** SHA-256 hex of an incoming opaque refresh token, used for DB lookup. */
    String hashRefreshToken(String rawToken);

    long refreshTtlSeconds();
}
