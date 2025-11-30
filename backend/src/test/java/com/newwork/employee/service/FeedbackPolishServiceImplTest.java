package com.newwork.employee.service;

import com.newwork.employee.config.HuggingFaceProperties;
import com.newwork.employee.dto.response.PolishFeedbackResponse;
import com.newwork.employee.exception.AiServiceException;
import com.newwork.employee.service.client.HuggingFaceClient;
import com.newwork.employee.service.impl.FeedbackPolishServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackPolishServiceImplTest {

    @Mock
    private HuggingFaceClient huggingFaceClient;

    private HuggingFaceProperties properties;

    @InjectMocks
    private FeedbackPolishServiceImpl feedbackPolishService;

    @BeforeEach
    void init() {
        properties = new HuggingFaceProperties();
        feedbackPolishService = new FeedbackPolishServiceImpl(huggingFaceClient, properties);
    }

    @Test
    void polish_ShouldReturnResponse() {
        when(huggingFaceClient.polish("Great teamwork!"))
                .thenReturn("Great teamwork! Keep it up.");

        PolishFeedbackResponse response = feedbackPolishService.polish("Great teamwork!");

        assertThat(response.getOriginalText()).isEqualTo("Great teamwork!");
        assertThat(response.getPolishedText()).isEqualTo("Great teamwork! Keep it up.");
    }

    @Test
    void polish_ShouldThrowWhenDisabled() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> feedbackPolishService.polish("Great teamwork!"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void polish_ShouldValidateMinimumLength() {
        assertThatThrownBy(() -> feedbackPolishService.polish("short"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
