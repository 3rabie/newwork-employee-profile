package com.newwork.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Feedback entity.
 * Used for API responses containing feedback information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {

    /**
     * Unique identifier for the feedback
     */
    private UUID id;

    /**
     * UUID of the employee who wrote the feedback
     */
    private UUID authorId;

    /**
     * Full name of the author (preferred name or legal name)
     */
    private String authorName;

    /**
     * UUID of the employee receiving the feedback
     */
    private UUID recipientId;

    /**
     * Full name of the recipient (preferred name or legal name)
     */
    private String recipientName;

    /**
     * The feedback text content
     */
    private String text;

    /**
     * Flag indicating if the feedback was polished by AI
     */
    private Boolean aiPolished;

    /**
     * Timestamp when the feedback was created
     */
    private LocalDateTime createdAt;
}
