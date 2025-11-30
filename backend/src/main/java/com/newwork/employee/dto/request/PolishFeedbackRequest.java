package com.newwork.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for polishing feedback text.
 */
@Data
public class PolishFeedbackRequest {

    @NotBlank(message = "Feedback text is required")
    @Size(min = 10, max = 1000, message = "Feedback text must be between 10 and 1000 characters")
    private String text;
}
