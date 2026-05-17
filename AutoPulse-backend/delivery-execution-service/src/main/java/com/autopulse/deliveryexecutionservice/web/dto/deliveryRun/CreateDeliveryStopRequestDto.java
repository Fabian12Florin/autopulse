package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateDeliveryStopRequestDto(
        @NotNull UUID routeStopId,
        @NotNull Integer stopOrder,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Integer parcelCount,
        @NotNull Double totalWeight,
        @NotNull Double totalVolume,
        @NotNull @Valid List<CreateDeliveryParcelRequestDto> parcels
) {}
