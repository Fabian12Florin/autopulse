package com.autopulse.parcelservice.web.dto.response;

import java.util.UUID;

public record RoutableParcelResponse(
        UUID parcelId,
        String awb,
        String receiverName,
        String receiverPhone,
        UUID receiverCityId,
        String receiverStreet,
        String receiverNumber,
        Double receiverLongitude,
        Double receiverLatitude,
        Double weight,
        Double volume
) {
}
