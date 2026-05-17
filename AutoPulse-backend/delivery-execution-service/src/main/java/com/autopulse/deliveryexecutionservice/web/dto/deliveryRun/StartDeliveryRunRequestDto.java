package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StartDeliveryRunRequestDto(
        @NotNull UUID routeId,
        @NotNull UUID courierId,
        @NotNull UUID vehicleId,
        @NotNull LocalDate routeDate,
        @NotBlank String googleMapsUrl,
        @NotNull Integer totalParcelCount,
        @NotNull Double totalWeight,
        @NotNull Double totalVolume,
        @NotEmpty @Valid List<CreateDeliveryStopRequestDto> stops
) {}
