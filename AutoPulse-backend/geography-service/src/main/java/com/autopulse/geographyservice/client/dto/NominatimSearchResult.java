package com.autopulse.geographyservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimSearchResult(
        String lat,
        String lon,
        @JsonProperty("display_name") String displayName
) {
}
