package com.autopulse.routingservice.vroom.dto.route;

import com.autopulse.routingservice.web.dto.parcel.RoutableParcelDto;

import java.util.List;

public record PlannedStop(
        int stopOrder,
        Double latitude,
        Double longitude,
        int parcelCount,
        int totalWeight,
        int totalVolume,
        List<RoutableParcelDto> parcels
) {
}
