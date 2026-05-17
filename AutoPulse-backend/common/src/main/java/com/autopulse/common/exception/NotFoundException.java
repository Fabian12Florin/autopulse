package com.autopulse.common.exception;

import java.util.Map;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(message, 404, "NOT_FOUND", Map.of());
    }

    public NotFoundException(String message, Map<String, Object> details) {
        super(message, 404, "NOT_FOUND", details);
    }
}
