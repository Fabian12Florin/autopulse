package com.autopulse.common.exception;

import java.util.Map;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(message, 409, "CONFLICT", Map.of());
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(message, 409, "CONFLICT", details);
    }
}