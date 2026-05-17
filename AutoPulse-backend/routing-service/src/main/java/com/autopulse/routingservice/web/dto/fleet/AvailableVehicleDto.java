package com.autopulse.routingservice.web.dto.fleet;

import java.util.UUID;

public record AvailableVehicleDto(
        UUID vehicleId,
        Integer capacityWeight,
        Integer capacityVolume
) {}