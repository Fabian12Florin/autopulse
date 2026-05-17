package com.autopulse.routingservice.mapper;

import com.autopulse.routingservice.model.RoutingJob;
import com.autopulse.routingservice.web.dto.RoutingJobResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoutingJobMapper {

    RoutingJobResponse toResponse(RoutingJob routingJob);
}