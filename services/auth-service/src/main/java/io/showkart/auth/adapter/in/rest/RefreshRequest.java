package io.showkart.auth.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        // Opaque token is a 32-byte random value in unpadded base64url = 43 chars exactly.
        @NotBlank @Size(min = 43, max = 43) String refreshToken
) { }
