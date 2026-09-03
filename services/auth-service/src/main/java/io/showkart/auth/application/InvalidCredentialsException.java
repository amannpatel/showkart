package io.showkart.auth.application;

/** Uniform "wrong credentials" signal. Never leak whether the email exists. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
