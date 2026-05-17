package com.autopulse.geographyservice.web.dto;

import java.time.Instant;
import java.util.UUID;

public record RegionResponse(
        UUID id,
        String name,
        String code,
        Instant createdAt,
        Instant updatedAt
) {
}
