package com.newwork.employee.exception;

/**
 * Thrown when provided authentication credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
