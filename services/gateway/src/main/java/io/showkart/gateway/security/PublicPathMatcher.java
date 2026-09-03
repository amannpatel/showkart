package io.showkart.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class PublicPathMatcher {

    private static final AntPathMatcher ANT = createMatcher();
    private static final List<String> ALWAYS_PUBLIC = List.of(
            "/api/v1/auth",
            "/api/v1/auth/**",
            "/api/v1/events",
            "/api/v1/events/**"
    );
    private static final String SHOWS = "/api/v1/shows/**";

    private static AntPathMatcher createMatcher() {
        AntPathMatcher m = new AntPathMatcher();
        m.setCaseSensitive(false);
        return m;
    }

    public boolean isPublic(ServerHttpRequest request) {
        // CORS preflight must always bypass auth so browsers can complete the round-trip.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return true;
        }
        String path = request.getPath().value();
        for (String pattern : ALWAYS_PUBLIC) {
            if (ANT.match(pattern, path)) {
                return true;
            }
        }
        // Shows browse (GET / HEAD) is public; mutations require a token.
        if (ANT.match(SHOWS, path)
                && (HttpMethod.GET.equals(request.getMethod()) || HttpMethod.HEAD.equals(request.getMethod()))) {
            return true;
        }
        return false;
    }
}
