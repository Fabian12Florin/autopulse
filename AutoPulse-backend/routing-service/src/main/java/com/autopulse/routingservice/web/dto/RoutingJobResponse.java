package com.autopulse.routingservice.web.dto;

import com.autopulse.routingservice.model.enums.RouteType;
import com.autopulse.routingservice.model.enums.RoutingJobStatus;

import java.time.LocalDate;
import java.util.UUID;

public record RoutingJobResponse(
        UUID id,
        UUID generatedByUserId,
        UUID depotId,
        LocalDate routeDate,
        RouteType routeType,
        RoutingJobStatus routingJobStatus,
        Integer inputParcelCount,
        Integer inputCourierCount,
        Integer inputVehicleCount,
        Integer assignedParcelCount,
        Integer unassignedParcelCount,
        Integer numberOfRoutes
) {
}