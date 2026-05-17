package com.autopulse.geographyservice.exception;

import com.autopulse.common.exception.ApiException;

import java.util.Map;

public class ExternalGeocodingException extends ApiException {

    public ExternalGeocodingException(String message) {
        super(message, 502, "EXTERNAL_GEOCODING_ERROR", Map.of());
    }

    public ExternalGeocodingException(String message, Map<String, Object> details) {
        super(message, 502, "EXTERNAL_GEOCODING_ERROR", details);
    }
}
