package com.autopulse.common.exception;

import java.util.Map;

public abstract class ApiException extends RuntimeException {
    private final int httpStatus;
    private final String code;
    private final Map<String, Object> details;

    protected ApiException(String message, int httpStatus, String code, Map<String, Object> details) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public Map<String, Object> getDetails() { return details; }
}