package com.taxi.trip.exception;

import com.taxi.trip.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.put(fe.getField(), fe.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", details);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(RestClientResponseException ex) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        String code = "UPSTREAM_ERROR";
        if (ex.getStatusCode().value() == 409) {
            status = HttpStatus.CONFLICT;
            code = "CONFLICT";
        }
        return error(status, code, "Upstream service error: " + ex.getStatusText(), null);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, Map<String, String> details) {
        ErrorResponse body = new ErrorResponse();
        body.setCode(code);
        body.setMessage(message);
        if (details != null) {
            body.setDetails(details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
