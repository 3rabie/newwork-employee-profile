package com.newwork.employee.service;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.dto.request.CreateFeedbackRequest;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
import com.newwork.employee.mapper.FeedbackMapper;
import com.newwork.employee.repository.FeedbackRepository;
import com.newwork.employee.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService Tests")
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    @InjectMocks
    private FeedbackService feedbackService;

    private User author;
    private User recipient;
    private User manager;
    private Feedback feedback;
    private FeedbackDTO feedbackDTO;

    @BeforeEach
    void setUp() {
        // Setup manager
        manager = User.builder()
                .id(UUID.randomUUID())
                .employeeId("MGR001")
                .email("manager@company.com")
                .password("hashedPassword")
                .role(Role.MANAGER)
                .manager(null)
                .build();

        // Setup author (employee)
        author = User.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP001")
                .email("employee1@company.com")
                .password("hashedPassword")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();

        // Setup recipient (another employee)
        recipient = User.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP002")
                .email("employee2@company.com")
                .password("hashedPassword")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();

        // Setup feedback entity
        feedback = new Feedback();
        feedback.setId(UUID.randomUUID());
        feedback.setAuthor(author);
        feedback.setRecipient(recipient);
        feedback.setText("Great work on the project!");
        feedback.setAiPolished(false);
        feedback.setCreatedAt(LocalDateTime.now());

        // Setup feedback DTO
        feedbackDTO = new FeedbackDTO();
        feedbackDTO.setId(feedback.getId());
        feedbackDTO.setAuthorId(author.getId());
        feedbackDTO.setAuthorName("Test Employee 1");
        feedbackDTO.setRecipientId(recipient.getId());
        feedbackDTO.setRecipientName("Test Employee 2");
        feedbackDTO.setText(feedback.getText());
        feedbackDTO.setAiPolished(false);
        feedbackDTO.setCreatedAt(feedback.getCreatedAt());
    }

    @Nested
    @DisplayName("Create Feedback Tests")
    class CreateFeedbackTests {

        @Test
        @DisplayName("Should create feedback successfully")
        void shouldCreateFeedbackSuccessfully() {
            // Given
            CreateFeedbackRequest request = new CreateFeedbackRequest();
            request.setRecipientId(recipient.getId());
            request.setText("Great work on the project!");
            request.setAiPolished(false);

            when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
            when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(feedback);
            when(feedbackMapper.toDTO(feedback)).thenReturn(feedbackDTO);

            // When
            FeedbackDTO result = feedbackService.createFeedback(author.getId(), request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAuthorId()).isEqualTo(author.getId());
            assertThat(result.getRecipientId()).isEqualTo(recipient.getId());
            assertThat(result.getText()).isEqualTo("Great work on the project!");
            assertThat(result.getAiPolished()).isFalse();

            verify(feedbackRepository).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Should throw exception when author not found")
        void shouldThrowExceptionWhenAuthorNotFound() {
            // Given
            CreateFeedbackRequest request = new CreateFeedbackRequest();
            request.setRecipientId(recipient.getId());
            request.setText("Great work!");

            when(userRepository.findById(author.getId())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> feedbackService.createFeedback(author.getId(), request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Author not found");

            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when recipient not found")
        void shouldThrowExceptionWhenRecipientNotFound() {
            // Given
            CreateFeedbackRequest request = new CreateFeedbackRequest();
            request.setRecipientId(recipient.getId());
            request.setText("Great work!");

            when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
            when(userRepository.findById(recipient.getId())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> feedbackService.createFeedback(author.getId(), request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Recipient not found");

            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to give feedback to self")
        void shouldThrowExceptionForSelfFeedback() {
            // Given
            CreateFeedbackRequest request = new CreateFeedbackRequest();
            request.setRecipientId(author.getId()); // Same as author
            request.setText("I'm great!");

            when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

            // When/Then
            assertThatThrownBy(() -> feedbackService.createFeedback(author.getId(), request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Cannot give feedback to yourself");

            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should default aiPolished to false when not provided")
        void shouldDefaultAiPolishedToFalse() {
            // Given
            CreateFeedbackRequest request = new CreateFeedbackRequest();
            request.setRecipientId(recipient.getId());
            request.setText("Great work!");
            request.setAiPolished(null); // Not provided

            when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
            when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(feedback);
            when(feedbackMapper.toDTO(feedback)).thenReturn(feedbackDTO);

            // When
            FeedbackDTO result = feedbackService.createFeedback(author.getId(), request);

            // Then
            assertThat(result.getAiPolished()).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Feedback For User Tests")
    class GetFeedbackForUserTests {

        @Test
        @DisplayName("Should get visible feedback for user")
        void shouldGetVisibleFeedbackForUser() {
            // Given
            when(userRepository.existsById(author.getId())).thenReturn(true);
            when(userRepository.existsById(recipient.getId())).thenReturn(true);
            when(feedbackRepository.findVisibleFeedbackForUser(author.getId(), recipient.getId()))
                    .thenReturn(List.of(feedback));
            when(feedbackMapper.toDTO(feedback)).thenReturn(feedbackDTO);

            // When
            List<FeedbackDTO> result = feedbackService.getFeedbackForUser(author.getId(), recipient.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(feedback.getId());
        }

        @Test
        @DisplayName("Should throw exception when viewer not found")
        void shouldThrowExceptionWhenViewerNotFound() {
            // Given
            when(userRepository.existsById(author.getId())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> feedbackService.getFeedbackForUser(author.getId(), recipient.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Viewer not found");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.existsById(author.getId())).thenReturn(true);
            when(userRepository.existsById(recipient.getId())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> feedbackService.getFeedbackForUser(author.getId(), recipient.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should return empty list when no visible feedback exists")
        void shouldReturnEmptyListWhenNoFeedbackExists() {
            // Given
            when(userRepository.existsById(author.getId())).thenReturn(true);
            when(userRepository.existsById(recipient.getId())).thenReturn(true);
            when(feedbackRepository.findVisibleFeedbackForUser(author.getId(), recipient.getId()))
                    .thenReturn(List.of());

            // When
            List<FeedbackDTO> result = feedbackService.getFeedbackForUser(author.getId(), recipient.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Feedback By Author Tests")
    class GetFeedbackByAuthorTests {

        @Test
        @DisplayName("Should get all feedback by author")
        void shouldGetFeedbackByAuthor() {
            // Given
            when(userRepository.existsById(author.getId())).thenReturn(true);
            when(feedbackRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId()))
                    .thenReturn(List.of(feedback));
            when(feedbackMapper.toDTO(feedback)).thenReturn(feedbackDTO);

            // When
            List<FeedbackDTO> result = feedbackService.getFeedbackByAuthor(author.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAuthorId()).isEqualTo(author.getId());
        }

        @Test
        @DisplayName("Should throw exception when author not found")
        void shouldThrowExceptionWhenAuthorNotFound() {
            // Given
            when(userRepository.existsById(author.getId())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> feedbackService.getFeedbackByAuthor(author.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Author not found");
        }
    }

    @Nested
    @DisplayName("Get Feedback By Recipient Tests")
    class GetFeedbackByRecipientTests {

        @Test
        @DisplayName("Should get all feedback by recipient")
        void shouldGetFeedbackByRecipient() {
            // Given
            when(userRepository.existsById(recipient.getId())).thenReturn(true);
            when(feedbackRepository.findByRecipientIdOrderByCreatedAtDesc(recipient.getId()))
                    .thenReturn(List.of(feedback));
            when(feedbackMapper.toDTO(feedback)).thenReturn(feedbackDTO);

            // When
            List<FeedbackDTO> result = feedbackService.getFeedbackByRecipient(recipient.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientId()).isEqualTo(recipient.getId());
        }

        @Test
        @DisplayName("Should throw exception when recipient not found")
        void shouldThrowExceptionWhenRecipientNotFound() {
            // Given
            when(userRepository.existsById(recipient.getId())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> feedbackService.getFeedbackByRecipient(recipient.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Recipient not found");
        }
    }
}
