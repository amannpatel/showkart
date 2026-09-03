package io.showkart.auth.application.port;

public interface PasswordEncoderPort {

    String encode(String rawPassword);
}
