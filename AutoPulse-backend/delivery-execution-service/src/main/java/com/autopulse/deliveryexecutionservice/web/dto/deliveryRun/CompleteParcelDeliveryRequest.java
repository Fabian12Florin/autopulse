package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import com.autopulse.deliveryexecutionservice.model.enums.DeliveryOutcome;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompleteParcelDeliveryRequest(
        @NotNull DeliveryOutcome outcome,
        String notes,
        BigDecimal paymentCollected
) {
    public CompleteParcelDeliveryRequest(DeliveryOutcome outcome) {
        this(outcome, null, null);
    }
}
