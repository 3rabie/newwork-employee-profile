package com.newwork.employee.service.client.impl;

import com.newwork.employee.config.properties.HuggingFaceProperties;
import com.newwork.employee.exception.AiServiceException;
import com.newwork.employee.service.client.HuggingFaceClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;

/**
 * HuggingFace inference API client implementation.
 */
@Component
public class HuggingFaceClientImpl implements HuggingFaceClient {

    private static final String POLISH_PROMPT = """
            You are a professional HR communication assistant.
            Rewrite employee-to-employee feedback so it is concise (max two sentences),
            warm, inclusive, and encouraging while keeping every original fact intact.
            Do not add questions, extra context, or speculative advice.
            Preserve the original language (English, German, etc.) and avoid corporate buzzwords.
            Return only the polished feedback text.
            """;

    private final RestClient restClient;
    private final HuggingFaceProperties properties;

    public HuggingFaceClientImpl(HuggingFaceProperties properties) {
        this.properties = properties;
        String apiUrl = Objects.requireNonNull(properties.getApiUrl(), "HuggingFace apiUrl must not be null");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
        }

        this.restClient = builder.build();
    }

    @Override
    public String polish(@NonNull String text) {
        try {
            String model = Objects.requireNonNull(properties.getModel(), "HuggingFace model must not be null");
            String sanitized = Objects.requireNonNull(text, "text must not be null");
            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", POLISH_PROMPT),
                    new ChatMessage("user", "Original feedback: \"" + sanitized + "\"")
            );

            ChatCompletionRequest request = new ChatCompletionRequest(
                    model,
                    false,
                    Objects.requireNonNull(messages, "messages must not be null")
            );

            ChatCompletionResponse response = restClient.post()
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiServiceException("Received empty response from AI service");
            }
            String polished = response.choices().get(0).message().content();
            if (polished == null || polished.isBlank()) {
                throw new AiServiceException("AI service returned empty completion");
            }
            return polished.trim();
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            throw new AiServiceException("AI service error: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new AiServiceException("AI service request failed", ex);
        }
    }

    private record ChatCompletionRequest(@NonNull String model,
                                         boolean stream,
                                         List<ChatMessage> messages) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }

    private record ChatMessage(@NonNull String role, @NonNull String content) {
    }
}
