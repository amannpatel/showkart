package io.showkart.auth.adapter.in.rest;

import java.util.UUID;

public record RegisterResponse(UUID userId, String email) {}
