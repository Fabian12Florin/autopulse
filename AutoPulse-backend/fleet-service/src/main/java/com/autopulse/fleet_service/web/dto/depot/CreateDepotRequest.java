package com.autopulse.fleet_service.web.dto.depot;

import com.autopulse.fleet_service.validation.Phone;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDepotRequest(
        @NotBlank
        @Size(min = 2, max = 150)
        String name,

        @NotBlank
        @Size(min = 2, max = 180)
        String addressStreet,

        @NotBlank
        @Size(min = 1, max = 40)
        String addressNumber,

        @NotNull
        UUID localityId,

        @Size(max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9_-]*$",
                message = "Locality code must contain only letters, digits, _ or -"
        )
        String localityCode,

        @NotBlank
        @Size(min = 2, max = 120)
        String contactName,

        @NotBlank
        @Phone
        @Size(max = 40)
        String contactPhone,

        @NotBlank
        @Email
        @Size(max = 180)
        String contactEmail,

        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(
                regexp = "^[A-Z0-9_-]+$",
                message = "Depot code must contain only uppercase letters, digits, _ or -"
        )
        String depotCode,

        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude
) {
}