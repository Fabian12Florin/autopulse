package com.autopulse.fleet_service.web.dto.vehicle_document;

import com.autopulse.fleet_service.model.VehicleDocumentType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateVehicleDocumentRequest(
        @NotNull
        VehicleDocumentType documentType,

        @NotNull
        @PastOrPresent
        LocalDate issuedAt,

        @NotNull
        @FutureOrPresent
        LocalDate expiresAt,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal cost,

        @Size(max = 500)
        String description
) {}