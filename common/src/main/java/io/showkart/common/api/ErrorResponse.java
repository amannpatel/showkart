package io.showkart.common.api;

public record ErrorResponse(String code, String message, String correlationId) {}
