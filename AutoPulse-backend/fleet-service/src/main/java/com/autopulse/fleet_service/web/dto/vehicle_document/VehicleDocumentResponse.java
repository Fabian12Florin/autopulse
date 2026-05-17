package com.autopulse.fleet_service.web.dto.vehicle_document;

import com.autopulse.fleet_service.model.VehicleDocumentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleDocumentResponse(
        UUID id,
        UUID vehicleId,
        VehicleDocumentType documentType,
        LocalDate issuedAt,
        LocalDate expiresAt,
        BigDecimal cost,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}