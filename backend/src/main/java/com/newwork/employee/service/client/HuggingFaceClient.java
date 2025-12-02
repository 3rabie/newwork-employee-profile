package com.newwork.employee.service.client;

import org.springframework.lang.NonNull;

/**
 * Client interface for HuggingFace inference API.
 * Abstracts AI text polishing for better testability and flexibility.
 */
public interface HuggingFaceClient {

    /**
     * Polish feedback text using AI to make it more professional and warm.
     *
     * @param text Original feedback text to polish (must not be null)
     * @return Polished feedback text
     * @throws com.newwork.employee.exception.AiServiceException if the AI service fails
     */
    String polish(@NonNull String text);
}
