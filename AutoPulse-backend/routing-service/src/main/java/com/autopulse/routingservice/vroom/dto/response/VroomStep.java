package com.autopulse.routingservice.vroom.dto.response;

import java.util.List;

public record VroomStep(
        String type,
        List<Double> location,
        Long id,
        Long job,
        Integer setup,
        Integer service,
        Integer waiting_time,
        List<Integer> load,
        Integer arrival,
        Integer duration
) {
}
