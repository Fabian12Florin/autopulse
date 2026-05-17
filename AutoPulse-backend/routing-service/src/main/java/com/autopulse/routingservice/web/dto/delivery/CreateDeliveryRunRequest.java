package com.autopulse.routingservice.web.dto.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateDeliveryRunRequest(
        @NotNull UUID routeId,
        @NotNull UUID courierId,
        @NotNull UUID vehicleId,
        @NotNull LocalDate routeDate,
        @NotBlank String googleMapsUrl,
        @NotNull Integer totalParcelCount,
        @NotNull Double totalWeight,
        @NotNull Double totalVolume,
        @NotNull @Valid List<CreateDeliveryStopRequest> stops
) {}
