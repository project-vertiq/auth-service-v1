package com.vertiq.auth.v1.controller;

import com.vertiq.auth.v1.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.warn("Handled RuntimeException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse();
        error.setError("Bad Request");
        error.setMessage(ex.getMessage());
        error.setStatus(400);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Handled validation error: {}", ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        ErrorResponse error = new ErrorResponse();
        error.setError("Validation Error");
        error.setMessage(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        error.setStatus(400);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unhandled exception: ", ex);
        ErrorResponse error = new ErrorResponse();
        error.setError("Internal Server Error");
        error.setMessage("An unexpected error occurred");
        error.setStatus(500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
