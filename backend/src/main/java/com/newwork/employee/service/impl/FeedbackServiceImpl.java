package com.newwork.employee.service.impl;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.dto.request.CreateFeedbackRequest;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
import com.newwork.employee.mapper.FeedbackMapper;
import com.newwork.employee.repository.FeedbackRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.FeedbackService;
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
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public FeedbackDTO createFeedback(UUID authorId, CreateFeedbackRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        if (authorId.equals(request.getRecipientId())) {
            throw new ForbiddenException("Cannot give feedback to yourself");
        }

        Feedback feedback = new Feedback();
        feedback.setAuthor(author);
        feedback.setRecipient(recipient);
        feedback.setText(request.getText());
        feedback.setAiPolished(request.getAiPolished() != null ? request.getAiPolished() : false);

        Feedback savedFeedback = feedbackRepository.save(feedback);
        return feedbackMapper.toDTO(savedFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getFeedbackForUser(UUID viewerId, UUID userId) {
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer not found"));

        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateFeedbackVisibility(viewer, recipient);

        List<Feedback> feedbackList = feedbackRepository.findVisibleFeedbackForUser(viewerId, userId);
        return feedbackList.stream()
                .map(feedbackMapper::toDTO)
                .toList();
    }

    @Override
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

    @Override
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

    private void validateFeedbackVisibility(User viewer, User recipient) {
        UUID viewerId = viewer.getId();
        UUID recipientId = recipient.getId();

        boolean isSelf = viewerId.equals(recipientId);
        boolean isManager = recipient.getManager() != null && recipient.getManager().getId().equals(viewerId);
        boolean isAuthor = false;

        if (!isSelf && !isManager) {
            isAuthor = feedbackRepository.existsByAuthorIdAndRecipientId(viewerId, recipientId);
        }

        if (!(isSelf || isManager || isAuthor)) {
            throw new ForbiddenException("You don't have permission to view feedback for this user");
        }
    }
}
