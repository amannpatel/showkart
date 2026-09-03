package io.showkart.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPathMatcherTest {

    private final PublicPathMatcher matcher = new PublicPathMatcher();

    @Test
    void auth_paths_are_public() {
        assertThat(matcher.isPublic(MockServerHttpRequest.post("/api/v1/auth/login").build())).isTrue();
        assertThat(matcher.isPublic(MockServerHttpRequest.post("/api/v1/auth/register").build())).isTrue();
        assertThat(matcher.isPublic(MockServerHttpRequest.post("/api/v1/auth/refresh").build())).isTrue();
    }

    @Test
    void events_paths_are_public() {
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/v1/events").build())).isTrue();
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/v1/events/42").build())).isTrue();
    }

    @Test
    void shows_get_is_public_but_mutations_are_protected() {
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/v1/shows/42").build())).isTrue();
        assertThat(matcher.isPublic(MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/shows").build())).isFalse();
        assertThat(matcher.isPublic(MockServerHttpRequest.method(HttpMethod.PUT, "/api/v1/shows/42").build())).isFalse();
        assertThat(matcher.isPublic(MockServerHttpRequest.method(HttpMethod.DELETE, "/api/v1/shows/42").build())).isFalse();
    }

    @Test
    void bookings_and_payments_are_protected() {
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/v1/bookings").build())).isFalse();
        assertThat(matcher.isPublic(MockServerHttpRequest.post("/api/v1/bookings").build())).isFalse();
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/v1/payments/xyz").build())).isFalse();
    }

    @Test
    void options_preflight_is_always_public() {
        assertThat(matcher.isPublic(MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/bookings").build())).isTrue();
        assertThat(matcher.isPublic(MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/payments").build())).isTrue();
    }

    @Test
    void head_on_shows_is_public_like_get() {
        assertThat(matcher.isPublic(MockServerHttpRequest.method(HttpMethod.HEAD, "/api/v1/shows/42").build())).isTrue();
    }

    @Test
    void uppercase_paths_are_matched_case_insensitively() {
        assertThat(matcher.isPublic(MockServerHttpRequest.post("/API/v1/AUTH/login").build())).isTrue();
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/V1/Events").build())).isTrue();
    }

    @Test
    void auth_root_without_trailing_slash_is_public() {
        assertThat(matcher.isPublic(MockServerHttpRequest.get("/api/v1/auth").build())).isTrue();
    }
}
