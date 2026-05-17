package com.autopulse.deliveryexecutionservice.web.dto.parcel;

import java.time.Instant;

public record ParcelRejectionResultRequest(
        Instant rejectedAt,
        String reason
) {
}
