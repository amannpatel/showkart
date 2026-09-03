package io.showkart.auth.application;

/**
 * Thrown when a registration collides with an existing account. The raw email is deliberately
 * NOT included in the exception message: at DEBUG the handler logs a generic line, and the
 * message is what would surface in any stack-trace-aware log path — keeping emails out of
 * exception messages avoids PII / enumeration leakage into logs (per Story 1.2 review).
 */
public class EmailTakenException extends RuntimeException {
    public EmailTakenException(String email) {
        super("Email is already registered.");
    }
}
