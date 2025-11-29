package com.newwork.employee.service;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.dto.request.CreateFeedbackRequest;

import java.util.List;
import java.util.UUID;

/**
 * Contract for feedback operations with permission enforcement.
 */
public interface FeedbackService {

    FeedbackDTO createFeedback(UUID authorId, CreateFeedbackRequest request);

    List<FeedbackDTO> getFeedbackForUser(UUID viewerId, UUID userId);

    List<FeedbackDTO> getFeedbackByAuthor(UUID authorId);

    List<FeedbackDTO> getFeedbackByRecipient(UUID recipientId);
}
