package io.showkart.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.showkart.gateway.security.JwtVerifier;
import io.showkart.gateway.security.PublicPathMatcher;
import io.jsonwebtoken.Claims;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthenticationGlobalFilter implements WebFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLES_HEADER = "X-Roles";
    public static final String ANONYMOUS = "anonymous";

    private final JwtVerifier verifier;
    private final PublicPathMatcher publicPaths;
    private final ObjectMapper json = new ObjectMapper();

    public JwtAuthenticationGlobalFilter(JwtVerifier verifier, PublicPathMatcher publicPaths) {
        this.verifier = verifier;
        this.publicPaths = publicPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Optional<Claims> maybeClaims = verifier.verify(authHeader);
        boolean isPublic = publicPaths.isPublic(exchange.getRequest());

        if (maybeClaims.isPresent()) {
            Claims claims = maybeClaims.get();
            String userId = claims.getSubject();
            String roles = Optional.ofNullable(claims.get("roles", String.class)).orElse("");
            return chain.filter(withIdentity(exchange, userId, roles));
        }

        // No valid token. Public paths pass through as anonymous; protected paths get 401.
        boolean tokenAttempted = authHeader != null && !authHeader.isBlank();
        if (isPublic && !tokenAttempted) {
            return chain.filter(withIdentity(exchange, ANONYMOUS, ""));
        }
        return write401(exchange);
    }

    private static ServerWebExchange withIdentity(ServerWebExchange exchange, String userId, String roles) {
        return exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove(USER_ID_HEADER);
                    h.remove(ROLES_HEADER);
                    h.set(USER_ID_HEADER, userId);
                    h.set(ROLES_HEADER, roles);
                }))
                .build();
    }

    private Mono<Void> write401(ServerWebExchange exchange) {
        // Read the correlation id from the request headers (CorrelationIdGlobalFilter mutated them
        // at HIGHEST_PRECEDENCE, so they are populated even if the response headers haven't been
        // committed yet); attribute lookup is a defensive fallback.
        String correlationId = Optional.ofNullable(
                        exchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER))
                .or(() -> Optional.ofNullable((String) exchange.getAttributes().get(CorrelationIdGlobalFilter.CONTEXT_KEY)))
                .orElse("unknown");
        if (exchange.getResponse().isCommitted()) {
            // Downstream already started writing; we cannot rewrite headers. Fail loudly.
            return Mono.error(new IllegalStateException("Cannot write 401: response already committed. correlationId=" + correlationId));
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "INVALID_TOKEN");
        body.put("message", "Access token is missing, invalid, or expired.");
        body.put("correlationId", correlationId);
        byte[] payload;
        try {
            payload = json.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            payload = ("{\"code\":\"INVALID_TOKEN\",\"correlationId\":\"" + correlationId + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(CorrelationIdGlobalFilter.HEADER, correlationId);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
