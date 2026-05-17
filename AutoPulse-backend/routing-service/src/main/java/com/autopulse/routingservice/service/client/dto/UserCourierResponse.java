package com.autopulse.routingservice.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserCourierResponse(
        UUID courierProfileId
) {
}
