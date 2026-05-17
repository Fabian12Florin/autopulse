package com.autopulse.parcelservice.web.dto.response;

import com.autopulse.parcelservice.model.enums.ParcelStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ParcelResponse(
        UUID id,
        String awb,
        String depotCode,
        ContactResponse senderContact,
        ContactResponse receiverContact,
        Double weight,
        Double volume,
        ParcelStatus status,
        boolean paymentRequired,
        BigDecimal paymentAmount,
        BigDecimal declaredValue,
        String observations,
        Instant createdAt,
        Instant updatedAt
) {
}

