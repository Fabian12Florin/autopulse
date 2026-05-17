package com.autopulse.fleet_service.web.dto.depot;

import java.time.Instant;
import java.util.UUID;

public record DepotResponse(
        UUID id,
        String name,
        String addressStreet,
        String addressNumber,
        UUID localityId,
        Double latitude,
        Double longitude,
        String localityCode,
        String contactName,
        String contactPhone,
        String contactEmail,
        String depotCode,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}