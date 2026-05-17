package com.autopulse.routingservice.vroom.dto.mapping;

import java.util.UUID;

public record SelectedParcelJob(
        long vroomJobId,
        UUID parcelId
) {}