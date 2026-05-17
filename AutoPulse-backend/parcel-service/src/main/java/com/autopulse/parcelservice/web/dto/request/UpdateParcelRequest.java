package com.autopulse.parcelservice.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateParcelRequest(
        @Size(max = 10) String depotCode,
        @Valid ContactRequest senderContact,
        @Valid ContactRequest receiverContact,
        @DecimalMin(value = "0.01") Double weight,
        @DecimalMin(value = "0.01") Double volume,
        Boolean paymentRequired,
        @DecimalMin(value = "0.00") BigDecimal paymentAmount,
        @DecimalMin(value = "0.00") BigDecimal declaredValue,
        @Size(max = 500) String observations
) {
}

