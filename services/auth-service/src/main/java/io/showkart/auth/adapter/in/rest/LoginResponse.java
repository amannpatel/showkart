package io.showkart.auth.adapter.in.rest;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {
}
