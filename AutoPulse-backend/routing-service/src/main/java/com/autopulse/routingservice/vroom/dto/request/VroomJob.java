package com.autopulse.routingservice.vroom.dto.request;

import java.util.List;

public record VroomJob(
        long id,
        List<Double> location,
        List<Integer> delivery
) {}