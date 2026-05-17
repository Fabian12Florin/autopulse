package com.autopulse.routingservice.web.dto.parcel;

import java.util.UUID;

public record RoutableParcelDto(
        UUID parcelId,
        String awb,
        String receiverName,
        String receiverPhone,
        Double deliveryLatitude,
        Double deliveryLongitude,
        Integer weight,
        Integer volume
) {}
