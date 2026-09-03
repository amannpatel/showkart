package io.showkart.auth.adapter.in.rest;

import io.showkart.auth.application.RegisterUserUseCase;
import io.showkart.auth.application.RegisterUserUseCase.Command;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class RegisterController {

    private final RegisterUserUseCase registerUser;

    RegisterController(RegisterUserUseCase registerUser) {
        this.registerUser = registerUser;
    }

    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        var result = registerUser.register(new Command(request.email(), request.password()));
        var body = new RegisterResponse(result.user().id(), result.user().email());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
