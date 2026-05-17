package com.autopulse.fleet_service.web.dto.vehicle;

import com.autopulse.fleet_service.model.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatusRequest(@NotNull VehicleStatus status) {}