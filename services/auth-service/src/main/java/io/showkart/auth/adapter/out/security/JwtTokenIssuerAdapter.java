package io.showkart.auth.adapter.out.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.showkart.auth.application.port.TokenIssuerPort;
import io.showkart.auth.domain.AccessToken;
import io.showkart.auth.domain.RefreshTokenValue;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private static final String ISSUER = "showkart-auth";

    private final SecretKey signingKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    JwtTokenIssuerAdapter(AuthTokenProperties props, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = props.getJwt().getAccessTtlSeconds();
        this.refreshTtlSeconds = props.getRefresh().getTtlSeconds();
        this.clock = clock;
    }

    @Override
    public AccessToken issueAccessToken(UUID userId, String roles) {
        Instant now = clock.instant();
        Instant exp = now.plusSeconds(accessTtlSeconds);
        String jwt = Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new AccessToken(jwt, accessTtlSeconds);
    }

    @Override
    public RefreshTokenValue issueRefreshToken() {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String opaque = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        return new RefreshTokenValue(opaque, sha256Hex(opaque));
    }

    @Override
    public String hashRefreshToken(String rawToken) {
        return sha256Hex(rawToken);
    }

    @Override
    public long refreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", e);
        }
    }
}
