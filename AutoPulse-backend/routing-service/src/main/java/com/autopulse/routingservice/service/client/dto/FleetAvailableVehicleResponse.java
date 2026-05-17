package com.autopulse.routingservice.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FleetAvailableVehicleResponse(
        UUID id,
        Double capacityVolume,
        Double capacityWeight
) {
}
