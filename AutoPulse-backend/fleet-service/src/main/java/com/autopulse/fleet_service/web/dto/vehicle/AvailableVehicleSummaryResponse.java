package com.autopulse.fleet_service.web.dto.vehicle;

import java.util.UUID;

public record AvailableVehicleSummaryResponse(
        UUID id,
        Double capacityVolume,
        Double capacityWeight
) {}