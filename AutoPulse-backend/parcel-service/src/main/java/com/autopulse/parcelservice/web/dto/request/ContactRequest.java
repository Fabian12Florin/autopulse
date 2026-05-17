package com.autopulse.parcelservice.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 120) String name,
        @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 40) String phone,
        @NotNull @Valid AddressRequest address
) {
}

