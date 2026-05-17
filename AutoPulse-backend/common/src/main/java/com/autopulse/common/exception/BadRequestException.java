package com.autopulse.common.exception;

import java.util.Map;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(message, 400, "BAD_REQUEST", Map.of());
    }

    public BadRequestException(String message, Map<String, Object> details) {
        super(message, 400, "BAD_REQUEST", details);
    }
}