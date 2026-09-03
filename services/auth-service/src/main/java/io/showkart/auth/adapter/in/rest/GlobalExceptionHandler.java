package io.showkart.auth.adapter.in.rest;

import io.showkart.common.api.ErrorResponse;
import io.showkart.auth.application.EmailTakenException;
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
     * Catches the race where two concurrent register calls both pass the pre-check and
     * the second one loses to the CITEXT unique constraint. Surfaces as 409 like the
     * regular duplicate path instead of leaking as 500 (per Story 1.2 review).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        LOG.debug("Registration blocked at DB layer: unique constraint hit (likely email race).");
        return response(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email is already registered.");
    }

    @ExceptionHandler(WeakPasswordException.class)
    ResponseEntity<ErrorResponse> handleWeakPassword(WeakPasswordException ex) {
        return response(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", ex.getMessage());
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
