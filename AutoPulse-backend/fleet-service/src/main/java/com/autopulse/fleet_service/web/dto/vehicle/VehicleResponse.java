package com.autopulse.fleet_service.web.dto.vehicle;

import com.autopulse.fleet_service.model.FuelType;
import com.autopulse.fleet_service.model.VehicleCategory;
import com.autopulse.fleet_service.model.VehicleStatus;
import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID depotId,
        String licensePlate,
        String brand,
        String model,
        String vin,
        Integer year,
        VehicleCategory category,
        FuelType fuelType,
        Double capacityWeight,
        Double capacityVolume,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}