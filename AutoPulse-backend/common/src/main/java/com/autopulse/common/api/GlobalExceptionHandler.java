package com.autopulse.common.api;

import com.autopulse.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {

        return ResponseEntity.status(ex.getHttpStatus())
                .body(build(request, ex.getHttpStatus(), ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParam(MissingServletRequestParameterException ex,
                                                                   HttpServletRequest request) {

        ErrorResponse body = build(
                request,
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                Map.of(
                        "parameterName", ex.getParameterName(),
                        "parameterType", ex.getParameterType()
                )
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        ErrorResponse body = build(
                request,
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                "Resource already exists",
                Map.of("cause", "DATA_INTEGRITY_VIOLATION")
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {

        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";

        ErrorResponse body = build(
                request,
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Invalid value for parameter '%s'. Expected type: %s.".formatted(ex.getName(), expectedType),
                Map.of(
                        "parameter", ex.getName(),
                        "expectedType", expectedType,
                        "value", value
                )
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        ErrorResponse body = build(
                request,
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Invalid request body",
                Map.of()
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                build(
                        request,
                        400,
                        "VALIDATION_ERROR",
                        "Validation failed",
                        Map.of()
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity.badRequest().body(
                build(
                        request,
                        400,
                        "VALIDATION_ERROR",
                        "Validation failed",
                        Map.of()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                        (a, b) -> a
                ));

        return ResponseEntity.badRequest()
                .body(build(request, 400, "VALIDATION_ERROR", "Validation failed", Map.of("fields", fields)));
    }

    private ErrorResponse build(HttpServletRequest request, int status, String code, String message, Map<String, Object> details) {
        String path = request.getRequestURI();
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        return new ErrorResponse(
                Instant.now(),
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                message,
                path,
                correlationId,
                code,
                details == null ? Map.of() : details
        );
    }
}
