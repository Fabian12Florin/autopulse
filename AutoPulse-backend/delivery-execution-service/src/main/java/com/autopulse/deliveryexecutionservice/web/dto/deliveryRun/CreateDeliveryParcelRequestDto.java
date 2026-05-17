package com.autopulse.deliveryexecutionservice.web.dto.deliveryRun;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDeliveryParcelRequestDto(
        @NotNull UUID parcelId,
        String awb,
        String receiverName,
        String receiverPhone,
        Integer weight,
        Integer volume
) {}
