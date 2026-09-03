package io.showkart.auth.domain;

/** The client-facing opaque string paired with the SHA-256 hex hash we persist. */
public record RefreshTokenValue(String raw, String sha256Hex) {
}
