package io.showkart.auth.adapter.out.security;

import io.showkart.auth.application.port.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private static final int STRENGTH = 12;

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return delegate.matches(rawPassword, storedHash);
    }
}
