package com.autopulse.routingservice.vroom.dto.route;

import java.util.List;
import java.util.UUID;

public record PlannedRoute(
        UUID vehicleId,
        UUID courierId,
        int totalParcelCount,
        int totalWeight,
        int totalVolume,
        String googleMapsUrl,
        List<PlannedStop> stops
) {
}
