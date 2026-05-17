package com.autopulse.parcelservice.web.dto.request;

import com.autopulse.parcelservice.model.enums.ParcelStatus;
import jakarta.validation.constraints.NotNull;

public record ParcelStatusUpdateRequest(
        @NotNull ParcelStatus status
) {
}