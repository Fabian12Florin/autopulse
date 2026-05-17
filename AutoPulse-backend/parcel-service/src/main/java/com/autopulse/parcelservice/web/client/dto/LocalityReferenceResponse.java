package com.autopulse.parcelservice.web.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalityReferenceResponse(
        UUID id,
        String name,
        String code,
        RegionReferenceResponse region
) {
}