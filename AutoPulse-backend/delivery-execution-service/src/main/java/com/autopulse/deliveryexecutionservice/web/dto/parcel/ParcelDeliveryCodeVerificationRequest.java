package com.autopulse.deliveryexecutionservice.web.dto.parcel;

import jakarta.validation.constraints.NotBlank;

public record ParcelDeliveryCodeVerificationRequest(
        @NotBlank String awb,
        @NotBlank String pin
) {}
