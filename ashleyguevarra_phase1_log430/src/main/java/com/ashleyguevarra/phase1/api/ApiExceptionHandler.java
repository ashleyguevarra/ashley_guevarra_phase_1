package com.ashleyguevarra.phase1.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {

        List<Map<String, String>> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "error", "VALIDATION_ERROR",
                "message", "Invalid request",
                "details", details
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {

        String code = ex.getMessage() == null ? "BAD_REQUEST" : ex.getMessage();

        HttpStatus status = "CUSTOMER_NOT_FOUND".equals(code)
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        String error = status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : code;

        if ("ACCOUNT_NOT_FOUND".equals(code)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now().toString(),
                "error", "ACCOUNT_NOT_FOUND",
                "message", "Account not found"
        ));
        }

        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "error", error,
                "message", code
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", Instant.now().toString(),
                "error", "CONFLICT",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> forbidden(SecurityException ex) {
        String code = ex.getMessage() == null ? "FORBIDDEN_RESOURCE" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", Instant.now().toString(),
                "error", code,
                "message", code
        ));
   }

   @ExceptionHandler(com.ashleyguevarra.phase1.api.ApiException.class)
   public org.springframework.http.ResponseEntity<?> handleApiException(com.ashleyguevarra.phase1.api.ApiException ex) {
        return org.springframework.http.ResponseEntity.status(ex.getStatus()).body(java.util.Map.of(
            "timestamp", java.time.Instant.now().toString(),
            "error", ex.getError(),
            "message", ex.getMessage()
        ));
   }
}