package com.autopulse.geographyservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLocalityRequest(
        @NotBlank(message = "Locality name must not be blank")
        @Size(min = 2, max = 120, message = "Locality name must be between 2 and 120 characters")
        String name,
        @NotBlank(message = "Locality code must not be blank")
        @Size(min = 2, max = 20, message = "Locality code must be between 2 and 20 characters")
        String code,
        @NotNull(message = "Region ID must not be null")
        UUID regionId
) {
}
