package io.showkart.auth.application;

import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;

public interface RefreshTokenUseCase {

    Result refresh(Command command);

    record Command(String rawRefreshToken) {}

    record Result(AccessToken accessToken, RefreshTokenValue refreshToken) {}
}
