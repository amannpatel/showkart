package io.showkart.auth.adapter.in.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email String email,
        // Cap at bcrypt's 72-byte limit so we don't spend hashing work on payloads bcrypt would truncate.
        @NotBlank @Size(min = 1, max = 72) String password
) { }
