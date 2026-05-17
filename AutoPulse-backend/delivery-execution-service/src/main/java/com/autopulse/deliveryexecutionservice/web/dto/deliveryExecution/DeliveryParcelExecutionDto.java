package com.autopulse.deliveryexecutionservice.web.dto.deliveryExecution;

import com.autopulse.deliveryexecutionservice.model.enums.DeliveryOutcome;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryParcelExecutionDto(
        UUID id,
        UUID parcelId,
        String awb,
        String receiverName,
        String receiverPhone,
        Integer weight,
        Integer volume,
        DeliveryOutcome outcome,
        Boolean deliveryCodeVerified,
        OffsetDateTime completedAt
) {
}
