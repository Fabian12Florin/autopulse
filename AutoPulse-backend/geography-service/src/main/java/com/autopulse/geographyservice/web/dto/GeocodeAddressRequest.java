package com.autopulse.geographyservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Address request used to resolve geographic coordinates through Nominatim.")
public record GeocodeAddressRequest(
        @NotBlank
        @Size(max = 200)
        @Schema(description = "Street name and optional street number", example = "Leandrului 1")
        String street,

        @NotBlank
        @Size(max = 120)
        @Schema(description = "City or locality name", example = "Timisoara")
        String city,

        @Size(max = 120)
        @Schema(description = "Optional county name used to narrow down the search", example = "Timis")
        String county
) {
}
