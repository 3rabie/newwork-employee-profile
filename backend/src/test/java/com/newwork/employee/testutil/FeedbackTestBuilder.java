package com.newwork.employee.testutil;

import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test builder for creating Feedback test data with sensible defaults.
 * Provides a fluent API for customizing test feedback.
 */
public class FeedbackTestBuilder {

    private UUID id = UUID.randomUUID();
    private User author = UserTestBuilder.aUser().withEmail("author@example.com").build();
    private User recipient = UserTestBuilder.aUser().withEmail("recipient@example.com").build();
    private String text = "Great work on the project! You showed excellent teamwork and technical skills.";
    private Boolean aiPolished = false;
    private LocalDateTime createdAt = LocalDateTime.now().minusDays(7);

    public static FeedbackTestBuilder aFeedback() {
        return new FeedbackTestBuilder();
    }

    public static FeedbackTestBuilder aFeedbackFrom(User author) {
        return new FeedbackTestBuilder().withAuthor(author);
    }

    public static FeedbackTestBuilder aFeedbackTo(User recipient) {
        return new FeedbackTestBuilder().withRecipient(recipient);
    }

    public FeedbackTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public FeedbackTestBuilder withAuthor(User author) {
        this.author = author;
        return this;
    }

    public FeedbackTestBuilder withRecipient(User recipient) {
        this.recipient = recipient;
        return this;
    }

    public FeedbackTestBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public FeedbackTestBuilder withAiPolished(Boolean aiPolished) {
        this.aiPolished = aiPolished;
        return this;
    }

    public FeedbackTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Feedback build() {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setAuthor(author);
        feedback.setRecipient(recipient);
        feedback.setText(text);
        feedback.setAiPolished(aiPolished);
        feedback.setCreatedAt(createdAt);
        return feedback;
    }
}
