package io.showkart.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtVerifierTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-bytes-long-yes-really";
    private static final String OTHER_SECRET = "totally-different-secret-of-sufficient-length-32-bytes";

    private JwtVerifier verifier;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret(SECRET);
        verifier = new JwtVerifier(props);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void valid_token_returns_claims_with_sub_and_roles() {
        String token = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject("00000000-0000-0000-0000-000000000001")
                .claim("roles", "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        Optional<Claims> claims = verifier.verify("Bearer " + token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(claims.get().get("roles", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    void missing_bearer_prefix_is_empty() {
        assertThat(verifier.verify(null)).isEmpty();
        assertThat(verifier.verify("")).isEmpty();
        assertThat(verifier.verify("Basic dXNlcjpwdw==")).isEmpty();
    }

    @Test
    void wrong_signature_is_empty() {
        SecretKey otherKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject("x")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }

    @Test
    void expired_token_is_empty() {
        String token = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject("x")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }

    @Test
    void wrong_issuer_is_empty() {
        String token = Jwts.builder()
                .issuer("totally-different-issuer")
                .subject("x")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }

    @Test
    void alg_none_is_rejected() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"iss\":\"" + AuthProperties.EXPECTED_ISSUER + "\",\"sub\":\"hacker\"}")
                        .getBytes(StandardCharsets.UTF_8));
        String token = header + "." + payload + ".";
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }

    @Test
    void garbage_token_is_empty() {
        assertThat(verifier.verify("Bearer not-a-jwt")).isEmpty();
        assertThat(verifier.verify("Bearer aaa.bbb.ccc")).isEmpty();
    }

    @Test
    void hs384_signed_token_is_rejected_even_with_same_key() {
        String token = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject("x")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS384)
                .compact();
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }

    @Test
    void missing_subject_is_rejected() {
        String token = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }

    @Test
    void blank_subject_is_rejected() {
        String token = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject("   ")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThat(verifier.verify("Bearer " + token)).isEmpty();
    }
}
