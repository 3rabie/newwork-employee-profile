package com.newwork.employee.service;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.dto.request.CreateFeedbackRequest;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
import com.newwork.employee.mapper.FeedbackMapper;
import com.newwork.employee.repository.FeedbackRepository;
import com.newwork.employee.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing feedback with permission checks.
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FeedbackMapper feedbackMapper;

    /**
     * Create new feedback from author to recipient.
     *
     * @param authorId The UUID of the feedback author
     * @param request The feedback creation request
     * @return The created feedback DTO
     * @throws ResourceNotFoundException if recipient not found
     * @throws ForbiddenException if trying to give feedback to self
     */
    @Transactional
    public FeedbackDTO createFeedback(UUID authorId, CreateFeedbackRequest request) {
        // Validate author and recipient exist
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        // Prevent self-feedback
        if (authorId.equals(request.getRecipientId())) {
            throw new ForbiddenException("Cannot give feedback to yourself");
        }

        // Create feedback entity
        Feedback feedback = new Feedback();
        feedback.setAuthor(author);
        feedback.setRecipient(recipient);
        feedback.setText(request.getText());
        feedback.setAiPolished(request.getAiPolished() != null ? request.getAiPolished() : false);

        // Save and return
        Feedback savedFeedback = feedbackRepository.save(feedback);
        return feedbackMapper.toDTO(savedFeedback);
    }

    /**
     * Get all feedback visible to the viewer for a specific user.
     *
     * Visibility rules:
     * - Authors can see feedback they wrote
     * - Recipients can see feedback written about them
     * - Managers can see feedback about their direct reports
     *
     * @param viewerId The UUID of the viewer requesting feedback
     * @param userId The UUID of the user whose feedback to retrieve
     * @return List of visible feedback
     * @throws ResourceNotFoundException if viewer or user not found
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getFeedbackForUser(UUID viewerId, UUID userId) {
        // Validate viewer and user exist
        if (!userRepository.existsById(viewerId)) {
            throw new ResourceNotFoundException("Viewer not found");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        // Fetch visible feedback using repository query
        List<Feedback> feedbackList = feedbackRepository.findVisibleFeedbackForUser(viewerId, userId);

        // Convert to DTOs
        return feedbackList.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }

    /**
     * Get all feedback written by a specific author.
     *
     * @param authorId The UUID of the author
     * @return List of feedback written by the author
     * @throws ResourceNotFoundException if author not found
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getFeedbackByAuthor(UUID authorId) {
        if (!userRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Author not found");
        }

        List<Feedback> feedbackList = feedbackRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
        return feedbackList.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }

    /**
     * Get all feedback received by a specific recipient.
     * Only callable by the recipient themselves, their manager, or authors of the feedback.
     *
     * @param recipientId The UUID of the recipient
     * @return List of feedback received by the recipient
     * @throws ResourceNotFoundException if recipient not found
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getFeedbackByRecipient(UUID recipientId) {
        if (!userRepository.existsById(recipientId)) {
            throw new ResourceNotFoundException("Recipient not found");
        }

        List<Feedback> feedbackList = feedbackRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return feedbackList.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }
}
