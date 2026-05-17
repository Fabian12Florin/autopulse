package com.autopulse.userservice.web.dto;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCourierAvailabilityRequest(
        @NotNull(message = "Availability status must not be null")
        AvailabilityStatus availabilityStatus
) {
}
