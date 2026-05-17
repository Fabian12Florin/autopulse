package com.autopulse.fleet_service.web.dto.depot;

import java.util.UUID;

public record DepotCoordinatesResponse(
        UUID id,
        Double latitude,
        Double longitude
) {
}