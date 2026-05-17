package com.autopulse.routingservice.vroom.dto.response;

import java.util.List;

public record VroomSummary(
        Integer cost,
        Integer routes,
        Integer unassigned,
        List<Integer> delivery,
        List<Integer> amount,
        List<Integer> pickup,
        Integer setup,
        Integer service,
        Integer duration,
        Integer waiting_time,
        Integer priority
) {
}
