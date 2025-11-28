package com.newwork.employee.graphql;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL controller for feedback queries.
 * Provides efficient data fetching with DataLoader to prevent N+1 queries.
 */
@Controller
@RequiredArgsConstructor
public class FeedbackGraphQLController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;

    /**
     * Get all feedback visible to the authenticated user for a specific user.
     * Visibility rules: authors see their feedback, recipients see feedback about them,
     * managers see feedback about their direct reports.
     *
     * @param userId the ID of the user whose feedback to retrieve
     * @param userDetails the authenticated user
     * @return list of visible feedback
     */
    @QueryMapping
    public List<FeedbackDTO> feedbackForUser(
            @Argument UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID viewerId = UUID.fromString(userDetails.getUsername());
        return feedbackService.getFeedbackForUser(viewerId, userId);
    }

    /**
     * Get all feedback authored by the authenticated user.
     *
     * @param userDetails the authenticated user
     * @return list of authored feedback
     */
    @QueryMapping
    public List<FeedbackDTO> myAuthoredFeedback(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID authorId = UUID.fromString(userDetails.getUsername());
        return feedbackService.getFeedbackByAuthor(authorId);
    }

    /**
     * Get all feedback received by the authenticated user.
     *
     * @param userDetails the authenticated user
     * @return list of received feedback
     */
    @QueryMapping
    public List<FeedbackDTO> myReceivedFeedback(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID recipientId = UUID.fromString(userDetails.getUsername());
        return feedbackService.getFeedbackByRecipient(recipientId);
    }

    /**
     * Schema mapping for Feedback.author field.
     * Resolves author user entity from feedback DTO.
     *
     * @param feedback the feedback DTO
     * @return author User entity
     */
    @SchemaMapping(typeName = "Feedback", field = "author")
    public User author(FeedbackDTO feedback) {
        return userRepository.findById(feedback.getAuthorId()).orElse(null);
    }

    /**
     * Schema mapping for Feedback.recipient field.
     * Resolves recipient user entity from feedback DTO.
     *
     * @param feedback the feedback DTO
     * @return recipient User entity
     */
    @SchemaMapping(typeName = "Feedback", field = "recipient")
    public User recipient(FeedbackDTO feedback) {
        return userRepository.findById(feedback.getRecipientId()).orElse(null);
    }

    /**
     * Schema mapping for User.profile field.
     * Resolves employee profile from user entity.
     *
     * @param user the user entity
     * @return EmployeeProfile entity (may be null)
     */
    @SchemaMapping(typeName = "User", field = "profile")
    public EmployeeProfile profile(User user) {
        return profileRepository.findByUserId(user.getId()).orElse(null);
    }
}
