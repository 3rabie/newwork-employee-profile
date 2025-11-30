package com.newwork.employee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PolishFeedbackResponse {
    private String originalText;
    private String polishedText;
}
