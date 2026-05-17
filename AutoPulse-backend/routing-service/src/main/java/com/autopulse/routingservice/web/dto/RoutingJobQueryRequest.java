package com.autopulse.routingservice.web.dto;

import com.autopulse.routingservice.model.enums.RouteType;
import com.autopulse.routingservice.model.enums.RoutingJobStatus;

import java.time.LocalDate;

public record RoutingJobQueryRequest(
        String depotCode,
        LocalDate routeDate,
        RouteType routeType,
        RoutingJobStatus status
) {
}
