package com.newwork.employee.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Configuration for HuggingFace inference API integration.
 */
@Slf4j
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
     * SECURITY: Must be configured via environment variable APP_AI_HF_API_KEY.
     * Never commit this value to version control.
     */
    private String apiKey;

    /**
     * Blocking timeout for REST calls.
     */
    private Duration timeout = Duration.ofSeconds(10);

    /**
     * Validates configuration on startup.
     * Ensures API key is configured when AI features are enabled.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (enabled && !StringUtils.hasText(apiKey)) {
            log.error("CRITICAL SECURITY: HuggingFace AI is enabled but API key is not configured!");
            log.error("Set environment variable APP_AI_HF_API_KEY or disable AI with APP_AI_HF_ENABLED=false");
            throw new IllegalStateException(
                "HuggingFace API key must be configured when AI features are enabled. " +
                "Set APP_AI_HF_API_KEY environment variable."
            );
        }

        if (enabled) {
            log.info("HuggingFace AI integration enabled with model: {}", model);
        } else {
            log.info("HuggingFace AI integration is disabled");
        }
    }
}
