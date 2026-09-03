package io.showkart.auth.domain;

public record AccessToken(String jwt, long expiresInSeconds) {
}
