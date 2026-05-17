package com.autopulse.userservice.web.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID keycloakUserId,
        UUID dispatcherProfileId,
        UUID courierProfileId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
