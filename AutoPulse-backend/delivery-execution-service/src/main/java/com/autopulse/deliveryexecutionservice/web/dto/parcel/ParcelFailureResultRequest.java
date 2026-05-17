package com.autopulse.deliveryexecutionservice.web.dto.parcel;

import java.time.Instant;

public record ParcelFailureResultRequest(
        Instant failedAt,
        String reason
) {
}
