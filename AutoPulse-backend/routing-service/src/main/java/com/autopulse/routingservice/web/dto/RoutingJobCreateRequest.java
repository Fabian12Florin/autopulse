package com.autopulse.routingservice.web.dto;

import com.autopulse.routingservice.model.enums.RouteType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RoutingJobCreateRequest(
        @NotNull UUID depotId,
        @NotNull LocalDate routeDate,
        @NotNull RouteType routeType
) {
}
