package com.autopulse.parcelservice.web.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoordinatesResponse(
        Double x,
        Double y
) {
}
