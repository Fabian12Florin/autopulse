package com.autopulse.parcelservice.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateParcelRequest(
        @NotNull @Size(max = 10) String depotCode,
        @NotNull @Valid ContactRequest senderContact,
        @NotNull @Valid ContactRequest receiverContact,
        @NotNull @DecimalMin(value = "0.01") Double weight,
        @NotNull @DecimalMin(value = "0.01") Double volume,
        boolean paymentRequired,
        @DecimalMin(value = "0.00") BigDecimal paymentAmount,
        @DecimalMin(value = "0.00") BigDecimal declaredValue,
        @Size(max = 500) String observations
) {
}

