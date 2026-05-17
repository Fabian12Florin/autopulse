package com.autopulse.parcelservice.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyDeliveryCodeRequest(
        @NotBlank String awb,
        @NotBlank String pin
) {
}
