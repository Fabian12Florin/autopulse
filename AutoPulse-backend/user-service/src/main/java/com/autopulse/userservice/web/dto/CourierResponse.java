package com.autopulse.userservice.web.dto;

import com.autopulse.userservice.model.enums.AvailabilityStatus;

import java.time.Instant;
import java.util.UUID;

public record CourierResponse(
        UUID courierProfileId,
        UUID depotId,
        String regionCode,
        AvailabilityStatus availabilityStatus,
        UserResponse user,
        Instant createdAt,
        Instant updatedAt
) {
}
