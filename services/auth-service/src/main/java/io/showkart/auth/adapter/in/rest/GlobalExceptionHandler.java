package io.showkart.auth.adapter.in.rest;

import io.showkart.common.api.ErrorResponse;
import io.showkart.auth.application.EmailTakenException;
import io.showkart.auth.application.InvalidCredentialsException;
import io.showkart.auth.application.InvalidRefreshTokenException;
import io.showkart.auth.application.WeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailTakenException.class)
    ResponseEntity<ErrorResponse> handleEmailTaken(EmailTakenException ex) {
        LOG.debug("Registration blocked: email already registered.");
        return response(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email is already registered.");
    }

    /**
     * Only maps the users.email uniqueness collision to EMAIL_TAKEN; other integrity
     * violations (refresh_token FK, etc.) fall through to the generic handler so they
     * do not silently masquerade as an email conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause().getMessage();
        if (detail != null && detail.toLowerCase().contains("email")) {
            LOG.debug("Registration blocked at DB layer: email uniqueness violation.");
            return response(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email is already registered.");
        }
        LOG.error("Data integrity violation on auth path", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    @ExceptionHandler(WeakPasswordException.class)
    ResponseEntity<ErrorResponse> handleWeakPassword(WeakPasswordException ex) {
        return response(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        LOG.debug("Login blocked: invalid credentials.");
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        LOG.debug("Refresh blocked: unknown, expired, revoked, or replayed token.");
        return response(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> handleBadInput(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Request payload is invalid.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        LOG.error("Unhandled exception on auth path", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private static ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        String correlationId = MDC.get("x-correlation-id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "unknown";
        }
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, correlationId));
    }
}
