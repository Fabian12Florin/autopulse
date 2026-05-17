package com.autopulse.geographyservice.web.dto;

import java.util.UUID;

public record RegionReferenceResponse(
        UUID id,
        String name,
        String code
) {
}
