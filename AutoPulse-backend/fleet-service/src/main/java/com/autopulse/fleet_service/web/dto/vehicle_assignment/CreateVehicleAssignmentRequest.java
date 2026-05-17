package com.autopulse.fleet_service.web.dto.vehicle_assignment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateVehicleAssignmentRequest(
        @NotNull
        UUID vehicleId,

        @NotNull
        UUID courierId
) {
}