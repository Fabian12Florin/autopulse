package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import jakarta.validation.constraints.NotBlank;

public record VerifyDeliveryCodeRequest(
        @NotBlank String awb,
        @NotBlank String pin
) {}
