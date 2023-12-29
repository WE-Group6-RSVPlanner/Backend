package com.rsvpplaner.controller;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import rsvplaner.v1.model.ErrorResponse;

@ControllerAdvice
public class ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
    private static final String INTERNAL_ERROR_MESSAGE =
            "encountered an error while trying to process your request, error ID: ";


    @ExceptionHandler(value = {ErrorResponseException.class})
    protected ResponseEntity<ErrorResponse> errorResponseExceptionHandler(
            ErrorResponseException ex, WebRequest request) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse().error(ex.getMessage()));
    }

    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<ErrorResponse> exceptionHandler(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse().error(INTERNAL_ERROR_MESSAGE + logWithRandomUuid(ex)));
    }

    private String logWithRandomUuid(Exception ex) {
        var errorID = UUID.randomUUID().toString();
        log.error(
                "encountered an error while serving a request: {} ({})",
                ex.getMessage(),
                errorID,
                ex);
        return errorID;
    }
}
