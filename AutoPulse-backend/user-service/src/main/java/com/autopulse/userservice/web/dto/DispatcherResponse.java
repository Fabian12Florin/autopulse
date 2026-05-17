package com.autopulse.userservice.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DispatcherResponse(
        UUID dispatcherProfileId,
        String regionCode,
        UserResponse user,
        Instant createdAt,
        Instant updatedAt
) {
}
