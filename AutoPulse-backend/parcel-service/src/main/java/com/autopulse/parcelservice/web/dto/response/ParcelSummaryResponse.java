package com.autopulse.parcelservice.web.dto.response;

import com.autopulse.parcelservice.model.enums.ParcelStatus;

import java.time.Instant;
import java.util.UUID;

public record ParcelSummaryResponse(
        UUID id,
        String awb,
        String depotCode,
        String receiverName,
        UUID receiverCityId,
        Double weight,
        Double volume,
        ParcelStatus status,
        Instant createdAt
) {
}

