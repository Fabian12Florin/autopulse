package com.autopulse.parcelservice.web.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactResponse(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 180) String email,
        @NotBlank @Size(max = 40) String phone,
        @NotNull @Valid AddressResponse address
) {
}
