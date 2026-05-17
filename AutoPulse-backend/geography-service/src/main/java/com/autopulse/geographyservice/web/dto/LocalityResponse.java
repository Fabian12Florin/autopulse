package com.autopulse.geographyservice.web.dto;

import java.time.Instant;
import java.util.UUID;

public record LocalityResponse(
        UUID id,
        String name,
        String code,
        RegionReferenceResponse region,
        Instant createdAt,
        Instant updatedAt
) {
}
