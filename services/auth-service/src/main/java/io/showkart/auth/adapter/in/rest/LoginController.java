package io.showkart.auth.adapter.in.rest;

import io.showkart.auth.application.LoginUseCase;
import io.showkart.auth.application.RefreshTokenUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class LoginController {

    private final LoginUseCase login;
    private final RefreshTokenUseCase refresh;

    LoginController(LoginUseCase login, RefreshTokenUseCase refresh) {
        this.login = login;
        this.refresh = refresh;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.Result result = login.login(new LoginUseCase.Command(request.email(), request.password()));
        return ResponseEntity.ok(new LoginResponse(
                result.accessToken().jwt(),
                result.refreshToken().raw(),
                result.accessToken().expiresInSeconds()
        ));
    }

    @PostMapping("/refresh")
    ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenUseCase.Result result = refresh.refresh(new RefreshTokenUseCase.Command(request.refreshToken()));
        return ResponseEntity.ok(new LoginResponse(
                result.accessToken().jwt(),
                result.refreshToken().raw(),
                result.accessToken().expiresInSeconds()
        ));
    }
}
