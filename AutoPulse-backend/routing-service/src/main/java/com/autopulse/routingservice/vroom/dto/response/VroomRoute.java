package com.autopulse.routingservice.vroom.dto.response;

import java.util.List;

public record VroomRoute(
        Long vehicle,
        Integer cost,
        List<Integer> delivery,
        List<Integer> amount,
        List<Integer> pickup,
        Integer setup,
        Integer service,
        Integer duration,
        Integer distance,
        Integer waiting_time,
        Integer priority,
        List<VroomStep> steps
) {
}