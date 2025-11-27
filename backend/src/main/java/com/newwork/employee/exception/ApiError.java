package com.newwork.employee.exception;

import java.time.Instant;

/**
 * Standard API error payload.
 *
 * @param status HTTP status code
 * @param message human readable message
 * @param timestamp time error was generated
 */
public record ApiError(int status, String message, Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now());
    }
}
