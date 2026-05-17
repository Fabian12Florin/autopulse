package com.autopulse.userservice.web.dto;

import java.util.UUID;

public record PasswordChangeResponse(
        UUID userId,
        String email,
        String message
) {
}
