package com.autopulse.deliveryexecutionservice.web.dto.parcel;

import java.time.Instant;

public record ParcelWaitingPickupRequest(
        Instant markedAt,
        String reason
) {
}
