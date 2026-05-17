package com.autopulse.routingservice.vroom.dto.request;

import java.util.List;

public record VroomVehicle(
        long id,
        List<Double> start,
        List<Double> end,
        //weight and volume
        List<Integer> capacity
) {}