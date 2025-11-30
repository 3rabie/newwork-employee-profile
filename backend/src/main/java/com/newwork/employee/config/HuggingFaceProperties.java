package com.newwork.employee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration for HuggingFace inference API integration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai.huggingface")
public class HuggingFaceProperties {

    /**
     * Toggle for enabling AI polishing.
     */
    private boolean enabled = true;

    /**
     * HuggingFace inference endpoint URL.
     */
    private String apiUrl = "https://router.huggingface.co/v1/chat/completions";

    /**
     * Model identifier for chat completion.
     */
    private String model = "meta-llama/Llama-3.1-8B-Instruct";

    /**
     * API key used for Authorization header.
     */
    private String apiKey = "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";

    /**
     * Blocking timeout for REST calls.
     */
    private Duration timeout = Duration.ofSeconds(10);
}
