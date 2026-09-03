package io.showkart.auth.application;

import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;

public interface LoginUseCase {

    Result login(Command command);

    record Command(String email, String password) {}

    record Result(AccessToken accessToken, RefreshTokenValue refreshToken) {}
}
