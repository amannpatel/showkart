package io.showkart.auth.application;

/** Uniform "unknown / expired / revoked / replayed" signal for the refresh flow. */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token is invalid.");
    }
}
