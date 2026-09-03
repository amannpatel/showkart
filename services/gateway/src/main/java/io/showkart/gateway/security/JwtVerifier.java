package io.showkart.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class JwtVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(JwtVerifier.class);
    private static final String BEARER = "Bearer ";
    private static final String REQUIRED_ALG = "HS256";

    private final SecretKey signingKey;

    public JwtVerifier(AuthProperties props) {
        this.signingKey = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** Returns claims on a fully valid signed JWT; empty on any parse/signature/alg/issuer/exp/sub failure. */
    public Optional<Claims> verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(AuthProperties.EXPECTED_ISSUER)
                    .build()
                    .parseSignedClaims(token);
            // Lock the accepted algorithm to HS256 exactly — verifyWith(SecretKey) would otherwise
            // accept HS384 / HS512 signed with the same key, which is an algorithm-confusion vector.
            String alg = jws.getHeader().getAlgorithm();
            if (!REQUIRED_ALG.equals(alg)) {
                LOG.debug("JWT rejected: algorithm {} is not {}.", alg, REQUIRED_ALG);
                return Optional.empty();
            }
            Claims claims = jws.getPayload();
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                LOG.debug("JWT rejected: missing or blank subject.");
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception ex) {
            LOG.debug("JWT verification failed: {} ({})", ex.getClass().getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }
}
