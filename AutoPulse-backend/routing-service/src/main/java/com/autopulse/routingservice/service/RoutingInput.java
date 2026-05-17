package com.autopulse.routingservice.service;

import com.autopulse.routingservice.web.dto.fleet.AvailableVehicleDto;
import com.autopulse.routingservice.web.dto.fleet.DepotDto;
import com.autopulse.routingservice.web.dto.parcel.RoutableParcelDto;
import com.autopulse.routingservice.web.dto.user.AvailableCourierDto;

import java.util.List;

public record RoutingInput(
        DepotDto depot,
        List<AvailableVehicleDto> availableVehicles,
        List<AvailableCourierDto> availableCouriers,
        List<RoutableParcelDto> routableParcels
) {
}
