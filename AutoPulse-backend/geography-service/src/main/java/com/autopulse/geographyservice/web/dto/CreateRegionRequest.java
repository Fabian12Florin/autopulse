package com.autopulse.geographyservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRegionRequest(
        @NotBlank(message = "Region name must not be blank")
        @Size(min = 2, max = 100, message = "Region name must be between 2 and 100 characters")
        String name,
        @NotBlank(message = "Region code must not be blank")
        @Size(min = 2, max = 20, message = "Region code must be between 2 and 20 characters")
        String code
) {
}
