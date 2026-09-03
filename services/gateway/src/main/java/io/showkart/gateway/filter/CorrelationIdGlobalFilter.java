package io.showkart.gateway.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdGlobalFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CONTEXT_KEY = "x-correlation-id";
    // Reject anything that could inject CRLF into logs or downstream headers.
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]{1,128}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = (incoming != null && SAFE.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, correlationId)))
                .build();
        mutated.getResponse().getHeaders().set(HEADER, correlationId);
        mutated.getAttributes().put(CONTEXT_KEY, correlationId);

        MDC.put(CONTEXT_KEY, correlationId);
        return chain.filter(mutated)
                .contextWrite(ctx -> ctx.put(CONTEXT_KEY, correlationId))
                .doFinally(sig -> MDC.remove(CONTEXT_KEY));
    }
}
