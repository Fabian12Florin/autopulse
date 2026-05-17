package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import com.autopulse.deliveryexecutionservice.model.enums.DeliveryRunStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryRunDto(
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
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
