package com.autopulse.parcelservice.web.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddressResponse(
        @NotNull UUID cityId,
        @NotBlank @Size(max = 180) String street,
        @NotBlank @Size(max = 40) String number,
        @NotNull Double longitude,
        @NotNull Double latitude
) {
}
