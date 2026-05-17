package com.autopulse.routingservice.web.dto.fleet;

import java.util.UUID;

public record DepotDto(
        UUID depotId,
        String depotCode,
        Double latitude,
        Double longitude
) {}