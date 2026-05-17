package com.autopulse.routingservice.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FleetDepotResponse(
        UUID id,
        String depotCode,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
