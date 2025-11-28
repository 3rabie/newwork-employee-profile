package com.newwork.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating feedback.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedbackRequest {

    /**
     * The UUID of the employee receiving the feedback
     */
    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    /**
     * The feedback text content
     */
    @NotBlank(message = "Feedback text is required")
    private String text;

    /**
     * Optional flag to indicate if AI polishing should be applied
     * Defaults to false if not provided
     */
    private Boolean aiPolished;
}
