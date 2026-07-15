package com.studentlife.studentlifejava.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;

@RestControllerAdvice
public class GlobalException {

    private static final Logger log = LoggerFactory.getLogger(GlobalException.class);

    // =========================================
    // DEV + PROD: Custom API exception
    // Used for controlled business errors
    // =========================================
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<String>> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ApiResponse<>(
                        ex.getStatus(),
                        false,
                        ex.getMessage(),
                        null
                ));
    }

    // =========================================
    // DEV + PROD: Validation errors (@Valid DTO)
    // =========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        400,
                        false,
                        message,
                        null
                ));
    }

    // =========================================
    // DEV + PROD: Validation errors on @RequestParam/@PathVariable
    // (@Valid only covers @RequestBody DTOs - constraints on bare method
    // parameters like @NotBlank @RequestParam need @Validated on the
    // controller class, which throws this instead of MethodArgumentNotValidException)
    // =========================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, false, message, null));
    }

    // =========================================
    // DEV + PROD: Invalid JSON or unknown fields
    // =========================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidRequestBody(
            HttpMessageNotReadableException ex
    ) {
        Throwable cause = ex.getMostSpecificCause();

        String message = "Invalid request body";

        if (cause instanceof UnrecognizedPropertyException unknowField) {
            message = "Unknow field is not allowed";
        }

        return ResponseEntity
                .status((HttpStatus.BAD_REQUEST))
                .body(new ApiResponse<>(
                        400,
                        false,
                        message,
                        null
                ));
    }

    // =========================================
    // PROD SAFETY NET
    // DEV: helps catch bugs early
    // PROD: hides internal details
    // =========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleUnexpected(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                        500,
                        false,
                        "Internal server error",
                        null
                ));
    }
}
