package com.autopulse.deliveryexecutionservice.web.dto.parcel;

import java.math.BigDecimal;
import java.time.Instant;

public record ParcelDeliveryResultRequest(
        Instant completedAt,
        Boolean codeVerified,
        String notes,
        BigDecimal paymentCollected
) {
}
