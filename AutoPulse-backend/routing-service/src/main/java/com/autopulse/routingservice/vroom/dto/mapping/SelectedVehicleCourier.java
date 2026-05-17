package com.autopulse.routingservice.vroom.dto.mapping;

import java.util.UUID;

public record SelectedVehicleCourier(
        long vroomVehicleId,
        UUID vehicleId,
        UUID courierId
) {}