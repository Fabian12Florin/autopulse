package com.autopulse.routingservice.vroom.dto.request;

import com.autopulse.routingservice.vroom.dto.mapping.SelectedParcelJob;
import com.autopulse.routingservice.vroom.dto.mapping.SelectedVehicleCourier;

import java.util.List;

public record VroomRequestData(
        VroomRequest request,
        List<SelectedVehicleCourier> vehicleMappings,
        List<SelectedParcelJob> jobMappings
) {
}
