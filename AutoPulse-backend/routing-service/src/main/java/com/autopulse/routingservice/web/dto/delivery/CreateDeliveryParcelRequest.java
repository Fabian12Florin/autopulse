package com.autopulse.routingservice.web.dto.delivery;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDeliveryParcelRequest(
        @NotNull UUID parcelId,
        String awb,
        String receiverName,
        String receiverPhone,
        Integer weight,
        Integer volume
) {}
