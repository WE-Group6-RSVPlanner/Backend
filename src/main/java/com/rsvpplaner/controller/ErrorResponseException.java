package com.rsvpplaner.controller;

import lombok.Getter;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorResponseException extends RuntimeException {
    final HttpStatus statusCode;

    public ErrorResponseException(HttpStatus httpStatus, String message) {
        super(message);
        this.statusCode = httpStatus;
    }

    public ErrorResponseException(HttpStatus httpStatus, String message, Object... objects) {
        super(MessageFormatter.arrayFormat(message, objects).getMessage());
        this.statusCode = httpStatus;
    }
}
