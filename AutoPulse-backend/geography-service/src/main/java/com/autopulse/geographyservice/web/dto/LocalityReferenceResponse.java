package com.autopulse.geographyservice.web.dto;

import java.util.UUID;

public record LocalityReferenceResponse(
        UUID id,
        String name,
        String code,
        RegionReferenceResponse region
) {
}
