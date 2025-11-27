package com.newwork.employee.exception;

/**
 * Thrown when a user lookup by email fails.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
