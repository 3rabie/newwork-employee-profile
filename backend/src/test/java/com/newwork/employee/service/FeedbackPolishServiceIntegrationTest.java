package com.newwork.employee.service;

import com.newwork.employee.config.properties.HuggingFaceProperties;
import com.newwork.employee.dto.response.PolishFeedbackResponse;
import com.newwork.employee.exception.AiServiceException;
import com.newwork.employee.service.client.HuggingFaceClient;
import com.newwork.employee.service.impl.FeedbackPolishServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AI-powered feedback polishing functionality.
 * Tests the complete flow from service layer to AI client with various scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Feedback Polish Service Integration Tests")
class FeedbackPolishServiceIntegrationTest {

    @Mock
    private HuggingFaceClient huggingFaceClient;

    private HuggingFaceProperties properties;
    private FeedbackPolishService polishService;

    @BeforeEach
    void setUp() {
        properties = new HuggingFaceProperties();
        properties.setEnabled(true);
        properties.setApiUrl("https://api.huggingface.co/models/...");
        properties.setModel("meta-llama/Llama-3.1-8B-Instruct");
        properties.setApiKey("test-key");

        polishService = new FeedbackPolishServiceImpl(huggingFaceClient, properties);
    }

    @Nested
    @DisplayName("Successful Polishing Scenarios")
    class SuccessfulPolishing {

        @Test
        @DisplayName("Should polish feedback and return both original and polished text")
        void polish_Success() {
            String originalText = "You did great on the project, I think you should keep it up!";
            String polishedText = "You did great on the project! Keep up the excellent work.";

            when(huggingFaceClient.polish(originalText)).thenReturn(polishedText);

            PolishFeedbackResponse response = polishService.polish(originalText);

            assertThat(response).isNotNull();
            assertThat(response.getOriginalText()).isEqualTo(originalText);
            assertThat(response.getPolishedText()).isEqualTo(polishedText);
            verify(huggingFaceClient, times(1)).polish(originalText);
        }

        @Test
        @DisplayName("Should handle long feedback text")
        void polish_LongText() {
            String longText = "Great teamwork! " + "You showed excellent collaboration skills. ".repeat(20);
            String longTextTrimmed = longText.trim();
            String polishedText = "Great teamwork! You consistently showed excellent collaboration skills.";

            when(huggingFaceClient.polish(longTextTrimmed)).thenReturn(polishedText);

            PolishFeedbackResponse response = polishService.polish(longText);

            assertThat(response).isNotNull();
            assertThat(response.getPolishedText()).isEqualTo(polishedText);
            verify(huggingFaceClient).polish(longTextTrimmed);
        }

        @Test
        @DisplayName("Should handle feedback with special characters")
        void polish_SpecialCharacters() {
            String textWithSpecialChars = "Great work on the Q&A session! Your expertise in C++ & Java really shone through.";
            String polishedText = "Great work on the Q&A session! Your C++ and Java expertise truly shone through.";

            when(huggingFaceClient.polish(textWithSpecialChars)).thenReturn(polishedText);

            PolishFeedbackResponse response = polishService.polish(textWithSpecialChars);

            assertThat(response.getPolishedText()).isEqualTo(polishedText);
        }
    }

    @Nested
    @DisplayName("Error Handling Scenarios")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw exception when AI service is disabled")
        void polish_ServiceDisabled() {
            properties.setEnabled(false);

            assertThatThrownBy(() -> polishService.polish("Great job!"))
                    .isInstanceOf(AiServiceException.class)
                    .hasMessageContaining("disabled");

            verify(huggingFaceClient, never()).polish(anyString());
        }

        @Test
        @DisplayName("Should throw exception for text below minimum length")
        void polish_TextTooShort() {
            String shortText = "ok";

            assertThatThrownBy(() -> polishService.polish(shortText))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(huggingFaceClient, never()).polish(anyString());
        }

        @Test
        @DisplayName("Should throw exception for null text")
        void polish_NullText() {
            assertThatThrownBy(() -> polishService.polish(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(huggingFaceClient, never()).polish(anyString());
        }

        @Test
        @DisplayName("Should throw exception for blank text")
        void polish_BlankText() {
            assertThatThrownBy(() -> polishService.polish("   "))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(huggingFaceClient, never()).polish(anyString());
        }

        @Test
        @DisplayName("Should propagate AI service exceptions")
        void polish_AiServiceError() {
            String text = "Great job on the presentation!";
            when(huggingFaceClient.polish(text))
                    .thenThrow(new AiServiceException("API rate limit exceeded"));

            assertThatThrownBy(() -> polishService.polish(text))
                    .isInstanceOf(AiServiceException.class)
                    .hasMessageContaining("rate limit");

            verify(huggingFaceClient).polish(text);
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogic {

        @Test
        @DisplayName("Should preserve original text even when polishing fails")
        void polish_PreservesOriginal() {
            String original = "Excellent work on the API design!";
            String polished = "Excellent work on the API design! Your architecture choices were spot-on.";

            when(huggingFaceClient.polish(original)).thenReturn(polished);

            PolishFeedbackResponse response = polishService.polish(original);

            assertThat(response.getOriginalText()).isEqualTo(original);
            assertThat(response.getPolishedText()).isNotEqualTo(original);
        }

        @Test
        @DisplayName("Should handle AI returning same text as input")
        void polish_UnchangedText() {
            String text = "Perfect feedback that needs no improvement.";

            when(huggingFaceClient.polish(text)).thenReturn(text);

            PolishFeedbackResponse response = polishService.polish(text);

            assertThat(response.getOriginalText()).isEqualTo(text);
            assertThat(response.getPolishedText()).isEqualTo(text);
        }

        @Test
        @DisplayName("Should return AI response as-is without modification")
        void polish_ReturnsAiResponseUnmodified() {
            String original = "Good work on the documentation!";
            String polishedText = "Good work on the documentation! Very thorough and well-structured.";

            when(huggingFaceClient.polish(original)).thenReturn(polishedText);

            PolishFeedbackResponse response = polishService.polish(original);

            assertThat(response.getPolishedText()).isEqualTo(polishedText);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class Configuration {

        @Test
        @DisplayName("Should respect enabled flag")
        void respectsEnabledFlag() {
            properties.setEnabled(true);

            when(huggingFaceClient.polish(anyString())).thenReturn("Polished text");

            PolishFeedbackResponse response = polishService.polish("Great teamwork!");

            assertThat(response).isNotNull();
            verify(huggingFaceClient).polish(anyString());
        }

        @Test
        @DisplayName("Should not call AI when disabled")
        void doesNotCallWhenDisabled() {
            properties.setEnabled(false);

            assertThatThrownBy(() -> polishService.polish("Great teamwork!"))
                    .isInstanceOf(AiServiceException.class);

            verify(huggingFaceClient, never()).polish(anyString());
        }
    }
}
