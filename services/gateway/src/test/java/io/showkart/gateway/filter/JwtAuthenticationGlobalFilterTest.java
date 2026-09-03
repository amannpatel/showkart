package io.showkart.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.showkart.gateway.security.AuthProperties;
import io.showkart.gateway.security.JwtVerifier;
import io.showkart.gateway.security.PublicPathMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static io.showkart.gateway.filter.JwtAuthenticationGlobalFilter.ANONYMOUS;
import static io.showkart.gateway.filter.JwtAuthenticationGlobalFilter.ROLES_HEADER;
import static io.showkart.gateway.filter.JwtAuthenticationGlobalFilter.USER_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationGlobalFilterTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-bytes-long-yes-really";
    private static final String USER = "11111111-1111-1111-1111-111111111111";

    private JwtAuthenticationGlobalFilter filter;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret(SECRET);
        filter = new JwtAuthenticationGlobalFilter(new JwtVerifier(props), new PublicPathMatcher());
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void public_path_without_token_gets_anonymous_downstream_headers() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerHttpRequest> captured = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturing(captured))).verifyComplete();

        assertThat(captured.get().getHeaders().getFirst(USER_ID_HEADER)).isEqualTo(ANONYMOUS);
        assertThat(captured.get().getHeaders().getFirst(ROLES_HEADER)).isEmpty();
    }

    @Test
    void public_path_ignores_smuggled_identity_headers() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .header(USER_ID_HEADER, "attacker-uuid")
                .header(ROLES_HEADER, "ROLE_ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerHttpRequest> captured = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturing(captured))).verifyComplete();

        assertThat(captured.get().getHeaders().getFirst(USER_ID_HEADER)).isEqualTo(ANONYMOUS);
        assertThat(captured.get().getHeaders().getFirst(ROLES_HEADER)).isEmpty();
    }

    @Test
    void protected_valid_token_forwards_user_id_and_roles() {
        String jwt = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject(USER)
                .claim("roles", "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerHttpRequest> captured = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capturing(captured))).verifyComplete();

        assertThat(captured.get().getHeaders().getFirst(USER_ID_HEADER)).isEqualTo(USER);
        assertThat(captured.get().getHeaders().getFirst(ROLES_HEADER)).isEqualTo("ROLE_USER");
    }

    @Test
    void protected_missing_token_returns_401_envelope() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        JsonNode body = new ObjectMapper().readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_TOKEN");
        assertThat(body.get("correlationId").asText()).isNotBlank();
    }

    @Test
    void protected_expired_token_returns_401() {
        String jwt = Jwts.builder()
                .issuer(AuthProperties.EXPECTED_ISSUER)
                .subject(USER)
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void public_path_with_invalid_token_still_returns_401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void alg_none_token_returns_401_through_filter() {
        String algNoneJwt = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJzaG93a2FydC1hdXRoIiwic3ViIjoiaGFja2VyIn0.";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + algNoneJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void wrong_issuer_token_returns_401_through_filter() {
        String jwt = Jwts.builder()
                .issuer("some-other-issuer")
                .subject(USER)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void post_shows_without_token_returns_401() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/shows").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void protected_401_response_has_correlation_id_header() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/bookings")
                .header("X-Correlation-Id", "req-corr-999")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, unusedChain())).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id"))
                .isEqualTo("req-corr-999");
    }

    private static WebFilterChain capturing(AtomicReference<ServerHttpRequest> captured) {
        return exchange -> {
            captured.set(exchange.getRequest());
            return Mono.empty();
        };
    }

    private static WebFilterChain unusedChain() {
        return exchange -> Mono.error(new AssertionError("chain must not be invoked on 401 path"));
    }
}
