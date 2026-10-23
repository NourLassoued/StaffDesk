package com.northpeak.hrms.service;

/** Raised when user input breaks a business rule; the message is shown to the user as is. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
