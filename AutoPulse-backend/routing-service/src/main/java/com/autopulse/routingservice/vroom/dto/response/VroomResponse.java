package com.autopulse.routingservice.vroom.dto.response;

import java.util.List;
import java.util.Map;

public record VroomResponse(
        Integer code,
        VroomSummary summary,
        List<Map<String, Object>> unassigned,
        List<VroomRoute> routes
) {
}