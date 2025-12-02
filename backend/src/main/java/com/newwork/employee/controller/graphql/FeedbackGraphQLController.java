package com.newwork.employee.controller.graphql;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.entity.User;
import com.newwork.employee.service.FeedbackService;
import com.newwork.employee.security.AuthenticatedUserAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL controller for feedback queries.
 * Provides efficient data fetching with DataLoader to prevent N+1 queries.
 * User.profile field resolution is handled by EmployeeGraphQLController.
 */
@Controller
@RequiredArgsConstructor
public class FeedbackGraphQLController {

    private final FeedbackService feedbackService;

    @QueryMapping
    public List<FeedbackDTO> feedbackForUser(@Argument UUID userId) {
        UUID viewerId = AuthenticatedUserAccessor.currentUserId();
        return feedbackService.getFeedbackForUser(viewerId, userId);
    }

    @QueryMapping
    public List<FeedbackDTO> myAuthoredFeedback() {
        UUID authorId = AuthenticatedUserAccessor.currentUserId();
        return feedbackService.getFeedbackByAuthor(authorId);
    }

    @QueryMapping
    public List<FeedbackDTO> myReceivedFeedback() {
        UUID recipientId = AuthenticatedUserAccessor.currentUserId();
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

}
