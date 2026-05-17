package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import com.autopulse.deliveryexecutionservice.model.enums.DeliveryRunStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DeliveryRunDetailsDto(
        UUID id,
        UUID routeId,
        UUID courierId,
        UUID vehicleId,
        LocalDate routeDate,
        String googleMapsUrl,
        Integer totalParcelCount,
        Double totalWeight,
        Double totalVolume,
        DeliveryRunStatus status,
        DeliveryStopExecutionDto currentStop,
        List<DeliveryStopExecutionDto> stops
) {}
