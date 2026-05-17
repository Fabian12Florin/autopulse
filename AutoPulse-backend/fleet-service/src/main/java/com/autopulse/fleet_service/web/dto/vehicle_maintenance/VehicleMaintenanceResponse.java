package com.autopulse.fleet_service.web.dto.vehicle_maintenance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleMaintenanceResponse(
        UUID id,
        UUID vehicleId,
        Integer mileage,
        LocalDate serviceDate,
        BigDecimal cost,
        String description,
        String serviceProvider,
        Instant createdAt,
        Instant updatedAt
) {}