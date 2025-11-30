package com.newwork.employee.service.impl;

import com.newwork.employee.config.HuggingFaceProperties;
import com.newwork.employee.dto.response.PolishFeedbackResponse;
import com.newwork.employee.exception.AiServiceException;
import com.newwork.employee.service.FeedbackPolishService;
import com.newwork.employee.service.client.HuggingFaceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FeedbackPolishServiceImpl implements FeedbackPolishService {

    private static final int MIN_CHARS = 10;

    private final HuggingFaceClient huggingFaceClient;
    private final HuggingFaceProperties properties;

    @Override
    public PolishFeedbackResponse polish(String text) {
        if (!properties.isEnabled()) {
            throw new AiServiceException("AI polishing feature is disabled");
        }
        String sanitized = text != null ? text.trim() : "";
        if (sanitized.length() < MIN_CHARS) {
            throw new IllegalArgumentException("Feedback text must be at least " + MIN_CHARS + " characters");
        }

        String polished = huggingFaceClient.polish(sanitized);
        if (!StringUtils.hasText(polished)) {
            throw new AiServiceException("AI service returned an empty response");
        }

        return new PolishFeedbackResponse(sanitized, polished);
    }
}
