package com.newwork.employee.exception;

/**
 * Exception thrown when a user attempts an action they don't have permission for.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
