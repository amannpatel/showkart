package io.showkart.auth.application;

import io.showkart.auth.domain.UserAccount;

public interface RegisterUserUseCase {

    Result register(Command command);

    record Command(String email, String password) {}

    record Result(UserAccount user) {}
}
