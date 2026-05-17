package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import com.autopulse.deliveryexecutionservice.model.enums.DeliveryStopStatus;
import com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution.DeliveryParcelExecutionDto;

import java.util.List;
import java.util.UUID;

public record DeliveryStopExecutionDto(
        UUID id,
        UUID routeStopId,
        Integer stopOrder,
        Double latitude,
        Double longitude,
        Integer parcelCount,
        Double totalWeight,
        Double totalVolume,
        DeliveryStopStatus status,
        List<DeliveryParcelExecutionDto> parcels
) {}
