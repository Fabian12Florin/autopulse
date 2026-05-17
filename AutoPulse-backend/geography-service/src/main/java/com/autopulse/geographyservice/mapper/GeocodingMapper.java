package com.autopulse.geographyservice.mapper;

import com.autopulse.geographyservice.client.dto.NominatimSearchResult;
import com.autopulse.geographyservice.exception.ExternalGeocodingException;
import com.autopulse.geographyservice.web.dto.CoordinatesResponse;

import java.util.Map;

public final class GeocodingMapper {

    private GeocodingMapper() {
    }

    public static CoordinatesResponse toCoordinatesResponse(NominatimSearchResult result) {
        Double latitude = parseCoordinate(result.lat(), "lat");
        Double longitude = parseCoordinate(result.lon(), "lon");
        return new CoordinatesResponse(longitude, latitude);
    }

    private static Double parseCoordinate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ExternalGeocodingException(
                    "Nominatim returned a missing coordinate value",
                    Map.of("field", fieldName)
            );
        }

        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ExternalGeocodingException(
                    "Nominatim returned an invalid coordinate value",
                    Map.of("field", fieldName)
            );
        }
    }
}
