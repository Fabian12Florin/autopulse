package com.autopulse.fleet_service.web.dto.vehicle_maintenance;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateVehicleMaintenanceRequest(
        @NotNull
        @PositiveOrZero
        @Max(2_000_000)
        Integer mileage,

        @NotNull
        @PastOrPresent
        LocalDate serviceDate,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal cost,

        @Size(max = 500)
        String description,

        @Size(max = 150)
        String serviceProvider
) {}