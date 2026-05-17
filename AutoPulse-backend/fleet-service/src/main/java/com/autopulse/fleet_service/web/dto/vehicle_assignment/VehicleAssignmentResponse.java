package com.autopulse.fleet_service.web.dto.vehicle_assignment;

import com.autopulse.fleet_service.model.VehicleAssignmentStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleAssignmentResponse(
        UUID id,
        UUID vehicleId,
        UUID courierId,
        LocalDateTime assignedAt,
        LocalDateTime unassignedAt,
        VehicleAssignmentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}