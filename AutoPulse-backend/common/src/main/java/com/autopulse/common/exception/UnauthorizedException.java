package com.autopulse.common.exception;

import java.util.Map;

public class UnauthorizedException extends ApiException {

    private static final int STATUS = 401;
    private static final String CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(message, STATUS, CODE, Map.of());
    }
}
