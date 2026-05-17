package com.autopulse.parcelservice.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddressRequest(
        @NotNull UUID cityId,
        @NotBlank @Size(max = 180) String street,
        @NotBlank @Size(max = 40) String number,
        Double longitude,
        Double latitude
) {
}