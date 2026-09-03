package io.showkart.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static io.showkart.gateway.filter.CorrelationIdGlobalFilter.HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void missing_header_mints_uuid_and_echoes_on_response() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/events").build());
        AtomicReference<String> downstream = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            downstream.set(ex.getRequest().getHeaders().getFirst(HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(downstream.get()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER)).isEqualTo(downstream.get());
    }

    @Test
    void safe_incoming_header_is_echoed_verbatim() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/events").header(HEADER, "req-abc-123").build());
        AtomicReference<String> downstream = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            downstream.set(ex.getRequest().getHeaders().getFirst(HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(downstream.get()).isEqualTo("req-abc-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER)).isEqualTo("req-abc-123");
    }

    @Test
    void unsafe_incoming_header_is_replaced_with_uuid() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/events").header(HEADER, "<script>alert(1)</script>").build());
        AtomicReference<String> downstream = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            downstream.set(ex.getRequest().getHeaders().getFirst(HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(downstream.get()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
