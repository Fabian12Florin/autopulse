package com.autopulse.routingservice.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParcelRoutableResponse(
        UUID parcelId,
        String awb,
        String receiverName,
        String receiverPhone,
        UUID receiverCityId,
        String receiverStreet,
        String receiverNumber,
        Double receiverLatitude,
        Double receiverLongitude,
        Double weight,
        Double volume
) {
}
