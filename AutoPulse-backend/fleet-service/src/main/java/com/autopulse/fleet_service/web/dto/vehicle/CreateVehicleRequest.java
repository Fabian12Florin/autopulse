package com.autopulse.fleet_service.web.dto.vehicle;

import com.autopulse.fleet_service.model.FuelType;
import com.autopulse.fleet_service.model.VehicleCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateVehicleRequest(
        @NotNull
        UUID depotId,

        @NotBlank
        @Size(min = 4, max = 20)
        @Pattern(
                regexp = "^[A-Za-z0-9 -]+$",
                message = "License plate must contain only letters, digits, spaces or -"
        )
        String licensePlate,

        @NotBlank
        @Size(min = 2, max = 80)
        String brand,

        @NotBlank
        @Size(min = 1, max = 80)
        String model,

        @NotBlank
        @Size(min = 17, max = 17)
        @Pattern(
                regexp = "^[A-HJ-NPR-Z0-9]{17}$",
                message = "VIN must have 17 characters and cannot contain I, O or Q"
        )
        String vin,

        @NotNull
        @Min(1900)
        @Max(2100)
        Integer year,

        @NotNull
        VehicleCategory category,

        @NotNull
        FuelType fuelType,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        Double capacityWeight,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        Double capacityVolume
) {}