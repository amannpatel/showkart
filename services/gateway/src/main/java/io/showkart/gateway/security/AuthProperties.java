package io.showkart.gateway.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    static final int MIN_SECRET_BYTES = 32;
    public static final String EXPECTED_ISSUER = "showkart-auth";

    private final Jwt jwt = new Jwt();

    public Jwt getJwt() { return jwt; }

    @PostConstruct
    void validate() {
        String secret = jwt.getSecret() == null ? "" : jwt.getSecret();
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "auth.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) for HS256; got " + bytes.length);
        }
        if (secret.trim().isEmpty()) {
            throw new IllegalStateException("auth.jwt.secret must not be blank / whitespace only.");
        }
    }

    public static class Jwt {
        private String secret;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }
}
