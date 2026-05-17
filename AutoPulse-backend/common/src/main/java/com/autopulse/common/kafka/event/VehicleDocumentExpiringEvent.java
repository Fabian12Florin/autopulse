package com.autopulse.common.kafka.event;

import java.time.LocalDate;
import java.util.UUID;

public record VehicleDocumentExpiringEvent(
        UUID documentId,
        UUID vehicleId,
        String documentType,
        LocalDate expiresAt,
        String recipientEmail,
        String recipientName
) {
}