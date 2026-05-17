package com.autopulse.userservice.web.dto;

import java.util.UUID;

public record PasswordResetResponse(
        UUID userId,
        String email,
        String message
) {
}
