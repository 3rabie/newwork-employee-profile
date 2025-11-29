package com.newwork.employee.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Shared helpers for date/time conversions between entity and DTO layers.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    /**
     * Convert a nullable {@link LocalDateTime} to {@link OffsetDateTime} using UTC.
     */
    public static OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atOffset(ZoneOffset.UTC) : null;
    }
}
