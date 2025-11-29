package com.newwork.employee.controller.graphql;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.security.AuthenticatedUser;
import com.newwork.employee.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @QueryMapping
    public List<FeedbackDTO> feedbackForUser(@Argument UUID userId) {
        UUID viewerId = extractUserId();
        return feedbackService.getFeedbackForUser(viewerId, userId);
    }

    @QueryMapping
    public List<FeedbackDTO> myAuthoredFeedback() {
        UUID authorId = extractUserId();
        return feedbackService.getFeedbackByAuthor(authorId);
    }

    @QueryMapping
    public List<FeedbackDTO> myReceivedFeedback() {
        UUID recipientId = extractUserId();
        return feedbackService.getFeedbackByRecipient(recipientId);
    }

    @SchemaMapping(typeName = "Feedback", field = "author")
    public CompletableFuture<User> author(FeedbackDTO feedback, org.dataloader.DataLoader<UUID, User> loader) {
        return loader.load(feedback.getAuthorId());
    }

    @SchemaMapping(typeName = "Feedback", field = "recipient")
    public CompletableFuture<User> recipient(FeedbackDTO feedback, org.dataloader.DataLoader<UUID, User> loader) {
        return loader.load(feedback.getRecipientId());
    }

    @SchemaMapping(typeName = "User", field = "profile")
    public CompletableFuture<EmployeeProfile> profile(User user, org.dataloader.DataLoader<UUID, EmployeeProfile> loader) {
        return loader.load(user.getId());
    }

    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated access to GraphQL feedback query");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getUserId();
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }
}
