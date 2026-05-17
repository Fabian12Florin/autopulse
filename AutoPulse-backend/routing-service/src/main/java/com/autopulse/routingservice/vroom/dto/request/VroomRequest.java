package com.autopulse.routingservice.vroom.dto.request;

import java.util.List;

public record VroomRequest(
        List<VroomVehicle> vehicles,
        List<VroomJob> jobs
) {}