package io.showkart.auth.adapter.out.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "auth")
public class AuthTokenProperties {

    /** HS256 requires a >= 256-bit key; we validate byte length at startup. */
    static final int MIN_SECRET_BYTES = 32;

    private final Jwt jwt = new Jwt();
    private final Refresh refresh = new Refresh();

    public Jwt getJwt() { return jwt; }
    public Refresh getRefresh() { return refresh; }

    @PostConstruct
    void validate() {
        byte[] bytes = jwt.getSecret() == null ? new byte[0] : jwt.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "auth.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) for HS256; got " + bytes.length);
        }
        if (jwt.getAccessTtlSeconds() <= 0 || refresh.getTtlSeconds() <= 0) {
            throw new IllegalStateException("TTL properties must be positive.");
        }
    }

    public static class Jwt {
        private String secret;
        private long accessTtlSeconds = 3600;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTtlSeconds() { return accessTtlSeconds; }
        public void setAccessTtlSeconds(long accessTtlSeconds) { this.accessTtlSeconds = accessTtlSeconds; }
    }

    public static class Refresh {
        private long ttlSeconds = 604800;

        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }
}
