package com.newwork.employee.service;

import com.newwork.employee.dto.response.PolishFeedbackResponse;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.exception.AiServiceException;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.FeedbackRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.client.HuggingFaceClient;
import com.newwork.employee.testutil.EmployeeProfileTestBuilder;
import com.newwork.employee.testutil.FeedbackTestBuilder;
import com.newwork.employee.testutil.UserTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * True integration tests for AI-powered feedback polishing functionality.
 * Uses Testcontainers for real PostgreSQL database and mocks HuggingFace HTTP client.
 * Tests the complete flow from service layer with real database persistence.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Feedback Polish Service True Integration Tests")
class FeedbackPolishTrueIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private FeedbackPolishService polishService;

    @MockBean
    private HuggingFaceClient huggingFaceClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository profileRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private User author;
    private User recipient;

    @BeforeEach
    void setUp() {
        // Clear database
        feedbackRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users and profiles using builders
        author = UserTestBuilder.aUser()
                .withEmail("author@test.com")
                .withEmployeeId("EMP-001")
                .build();
        author = userRepository.save(author);

        recipient = UserTestBuilder.aUser()
                .withEmail("recipient@test.com")
                .withEmployeeId("EMP-002")
                .build();
        recipient = userRepository.save(recipient);

        EmployeeProfile authorProfile = EmployeeProfileTestBuilder.aProfileFor(author)
                .withPreferredName("Test Author")
                .build();
        profileRepository.save(authorProfile);

        EmployeeProfile recipientProfile = EmployeeProfileTestBuilder.aProfileFor(recipient)
                .withPreferredName("Test Recipient")
                .build();
        profileRepository.save(recipientProfile);
    }

    @Nested
    @DisplayName("End-to-End Polishing Scenarios")
    class EndToEndPolishing {

        @Test
        @DisplayName("Should polish feedback and persist to database")
        void polish_EndToEnd_Success() {
            String originalText = "You did great on the project, I think you should keep it up!";
            String polishedText = "You did great on the project! Keep up the excellent work.";

            // Mock HuggingFace client response
            when(huggingFaceClient.polish(originalText)).thenReturn(polishedText);

            // Execute polish operation
            PolishFeedbackResponse response = polishService.polish(originalText);

            // Verify response
            assertThat(response).isNotNull();
            assertThat(response.getOriginalText()).isEqualTo(originalText);
            assertThat(response.getPolishedText()).isEqualTo(polishedText);

            // Now create feedback with polished text using test builder
            Feedback feedback = FeedbackTestBuilder.aFeedback()
                    .withAuthor(author)
                    .withRecipient(recipient)
                    .withText(polishedText)
                    .withAiPolished(true)
                    .build();

            Feedback saved = feedbackRepository.save(feedback);

            // Verify persistence in real database
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAuthor().getId()).isEqualTo(author.getId());
            assertThat(saved.getRecipient().getId()).isEqualTo(recipient.getId());
            assertThat(saved.getText()).isEqualTo(polishedText);
            assertThat(saved.getAiPolished()).isTrue();
        }

        @Test
        @DisplayName("Should handle long feedback text with polishing")
        void polish_LongFeedback() {
            String longText = "Great teamwork! " + "You showed excellent collaboration skills. ".repeat(20);
            String polishedText = "Great teamwork! You consistently demonstrated excellent collaboration skills.";

            when(huggingFaceClient.polish(longText.trim())).thenReturn(polishedText);

            PolishFeedbackResponse response = polishService.polish(longText);

            assertThat(response.getOriginalText()).isEqualTo(longText.trim());
            assertThat(response.getPolishedText()).isEqualTo(polishedText);
        }

        @Test
        @DisplayName("Should handle special characters in feedback")
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
    class ErrorHandlingIntegration {

        @Test
        @DisplayName("Should throw exception when AI service fails")
        void polish_AiServiceError() {
            String text = "Great job on the presentation!";

            when(huggingFaceClient.polish(text))
                    .thenThrow(new AiServiceException("AI service error: rate limit exceeded"));

            assertThatThrownBy(() -> polishService.polish(text))
                    .isInstanceOf(AiServiceException.class)
                    .hasMessageContaining("rate limit exceeded");
        }

        @Test
        @DisplayName("Should throw exception when AI service returns empty content")
        void polish_EmptyAiResponse() {
            String text = "Great teamwork on the project!";

            when(huggingFaceClient.polish(text))
                    .thenThrow(new AiServiceException("AI service returned empty completion"));

            assertThatThrownBy(() -> polishService.polish(text))
                    .isInstanceOf(AiServiceException.class)
                    .hasMessageContaining("empty completion");
        }

        @Test
        @DisplayName("Should throw exception for text below minimum length")
        void polish_TextTooShort() {
            assertThatThrownBy(() -> polishService.polish("ok"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 10 characters");
        }

        @Test
        @DisplayName("Should throw exception for null text")
        void polish_NullText() {
            assertThatThrownBy(() -> polishService.polish(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception for blank text")
        void polish_BlankText() {
            assertThatThrownBy(() -> polishService.polish("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Database Integration Scenarios")
    class DatabaseIntegration {

        @Test
        @DisplayName("Should create and retrieve feedback with polished text")
        void createAndRetrieveFeedback() {
            String originalText = "Nice job on the feature implementation!";
            String polishedText = "Nice job on the feature implementation! Your attention to detail was impressive.";

            when(huggingFaceClient.polish(originalText)).thenReturn(polishedText);

            // Polish the text
            PolishFeedbackResponse polishResponse = polishService.polish(originalText);

            // Create feedback with polished text using test builder
            Feedback feedback = FeedbackTestBuilder.aFeedback()
                    .withAuthor(author)
                    .withRecipient(recipient)
                    .withText(polishResponse.getPolishedText())
                    .withAiPolished(true)
                    .build();

            Feedback saved = feedbackRepository.save(feedback);

            // Retrieve from real database
            Feedback retrieved = feedbackRepository.findById(saved.getId()).orElseThrow();

            assertThat(retrieved.getText()).isEqualTo(polishedText);
            assertThat(retrieved.getAiPolished()).isTrue();
            assertThat(retrieved.getAuthor().getId()).isEqualTo(author.getId());
            assertThat(retrieved.getRecipient().getId()).isEqualTo(recipient.getId());
        }

        @Test
        @DisplayName("Should handle multiple feedback polishing requests in sequence")
        void multiplePolishRequests() {
            String text1 = "Great work on the frontend!";
            String text2 = "Excellent backend implementation!";
            String polished1 = "Great work on the frontend! Your UI design was clean and intuitive.";
            String polished2 = "Excellent backend implementation! Your API design was well-structured.";

            when(huggingFaceClient.polish(text1)).thenReturn(polished1);
            when(huggingFaceClient.polish(text2)).thenReturn(polished2);

            // Execute both polish operations
            PolishFeedbackResponse response1 = polishService.polish(text1);
            PolishFeedbackResponse response2 = polishService.polish(text2);

            assertThat(response1.getPolishedText()).isEqualTo(polished1);
            assertThat(response2.getPolishedText()).isEqualTo(polished2);

            // Create and persist both feedbacks using test builders
            Feedback feedback1 = FeedbackTestBuilder.aFeedback()
                    .withAuthor(author)
                    .withRecipient(recipient)
                    .withText(polished1)
                    .withAiPolished(true)
                    .build();

            Feedback feedback2 = FeedbackTestBuilder.aFeedback()
                    .withAuthor(recipient)
                    .withRecipient(author)
                    .withText(polished2)
                    .withAiPolished(true)
                    .build();

            feedbackRepository.save(feedback1);
            feedbackRepository.save(feedback2);

            // Verify both persisted in database
            assertThat(feedbackRepository.count()).isEqualTo(2);
        }
    }
}
