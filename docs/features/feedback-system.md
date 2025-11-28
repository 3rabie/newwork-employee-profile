# Feedback System

## Overview

The Feedback System allows employees to give and receive peer feedback. This feature implements privacy controls ensuring that feedback is only visible to authorized users.

## Features

- **Create Feedback**: Employees can give feedback to their peers
- **View Feedback**: Users can view feedback based on their relationship (author, recipient, or manager)
- **Privacy Controls**: Strict visibility rules ensure feedback privacy
- **AI Polish Flag**: Optional flag to indicate AI-enhanced feedback

## Database Schema

### feedback Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique feedback identifier |
| author_id | UUID | NOT NULL, FK(users.id) | Employee who wrote the feedback |
| recipient_id | UUID | NOT NULL, FK(users.id) | Employee receiving the feedback |
| text | TEXT | NOT NULL, LENGTH >= 1 | Feedback content |
| ai_polished | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether feedback was AI-enhanced |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |

**Constraints:**
- `chk_feedback_text_length`: Ensures text is not empty
- `chk_feedback_not_self`: Prevents self-feedback (author_id != recipient_id)
- Foreign keys cascade on delete

**Indexes:**
- `idx_feedback_author_id`: Query feedback by author
- `idx_feedback_recipient_id`: Query feedback by recipient
- `idx_feedback_created_at`: Query feedback by date (DESC)

## Visibility Rules

Feedback visibility follows strict privacy rules:

1. **Authors** can see feedback they wrote
2. **Recipients** can see feedback written about them
3. **Managers** can see feedback about their direct reports
4. **Others** cannot see feedback (returns empty list)

### Visibility Query

The `FeedbackRepository.findVisibleFeedbackForUser(viewerId, userId)` method implements these rules using a JPQL query:

```sql
SELECT f FROM Feedback f
LEFT JOIN f.recipient r
WHERE f.recipient.id = :userId
AND (
    f.author.id = :viewerId           -- Viewer is author
    OR f.recipient.id = :viewerId      -- Viewer is recipient
    OR r.managerId = :viewerId         -- Viewer is recipient's manager
)
ORDER BY f.createdAt DESC
```

## API Endpoints

### POST /api/feedback

Create new feedback.

**Authentication:** Required

**Request Body:**
```json
{
  "recipientId": "uuid",
  "text": "Feedback text",
  "aiPolished": false
}
```

**Validation:**
- `recipientId`: Required, must be valid user UUID
- `text`: Required, not blank
- `aiPolished`: Optional, defaults to false

**Business Rules:**
- Cannot give feedback to self (returns 403 Forbidden)
- Recipient must exist (returns 404 Not Found)

**Response:** 201 Created
```json
{
  "id": "uuid",
  "authorId": "uuid",
  "authorName": "Author Display Name",
  "recipientId": "uuid",
  "recipientName": "Recipient Display Name",
  "text": "Feedback text",
  "aiPolished": false,
  "createdAt": "2024-01-15T10:30:00"
}
```

### GET /api/profiles/{id}/feedback

Get all feedback visible to the authenticated user for a specific profile.

**Authentication:** Required

**Path Parameters:**
- `id`: User UUID whose feedback to retrieve

**Visibility:** Returns only feedback visible based on viewer's relationship (author, recipient, or manager)

**Response:** 200 OK
```json
[
  {
    "id": "uuid",
    "authorId": "uuid",
    "authorName": "Author Display Name",
    "recipientId": "uuid",
    "recipientName": "Recipient Display Name",
    "text": "Feedback text",
    "aiPolished": false,
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

### GET /api/feedback/authored

Get all feedback written by the authenticated user.

**Authentication:** Required

**Response:** 200 OK (same format as above)

### GET /api/feedback/received

Get all feedback received by the authenticated user.

**Authentication:** Required

**Response:** 200 OK (same format as above)

## Architecture

### Entity Layer

**Feedback Entity** (`com.newwork.employee.entity.Feedback`)
- JPA entity with relationships to User (author and recipient)
- Lazy loading for performance
- `@PrePersist` callback sets timestamps and defaults

### Repository Layer

**FeedbackRepository** (`com.newwork.employee.repository.FeedbackRepository`)
- Extends `JpaRepository<Feedback, UUID>`
- Custom query methods:
  - `findByAuthorIdOrderByCreatedAtDesc`
  - `findByRecipientIdOrderByCreatedAtDesc`
  - `findVisibleFeedbackForUser` (JPQL with visibility logic)
  - `countByRecipientId` / `countByAuthorId`

### Service Layer

**FeedbackService** (`com.newwork.employee.service.FeedbackService`)
- Business logic and permission checks
- Validates users exist before operations
- Prevents self-feedback
- Uses `@Transactional` for data consistency

### Controller Layer

**FeedbackController** (`com.newwork.employee.controller.FeedbackController`)
- REST endpoints for feedback operations
- Uses `@AuthenticationPrincipal` for authenticated user
- Input validation with `@Valid`
- Returns appropriate HTTP status codes

### DTO Layer

**CreateFeedbackRequest** - Input DTO with validation
**FeedbackDTO** - Output DTO with display names

### Mapper Layer

**FeedbackMapper** - Converts Feedback entities to DTOs
- Fetches employee profiles for display names
- Prefers preferred name over legal name

## Security

- All endpoints require authentication (JWT Bearer token)
- User identity extracted from `UserDetails.getUsername()` (contains UUID)
- Authorization enforced at service layer through visibility queries
- No endpoints allow direct feedback deletion or editing (immutable after creation)

## Testing

### Unit Tests

**FeedbackServiceTest** (`com.newwork.employee.service.FeedbackServiceTest`)
- Tests all service methods with mocked dependencies
- Validates error handling (self-feedback, missing users)
- Verifies visibility logic
- Uses JUnit 5 with Mockito

### Integration Tests

**FeedbackControllerIntegrationTest** (`com.newwork.employee.controller.FeedbackControllerIntegrationTest`)
- Full-stack tests with Testcontainers PostgreSQL
- Tests all endpoints with real authentication
- Validates visibility rules with multiple users
- Verifies authorization and permission logic

## Future Enhancements

Potential improvements for future iterations:

1. **AI Polishing**: Integrate AI service to enhance feedback text
2. **Edit/Delete**: Allow authors to edit/delete within time window
3. **Categories**: Add feedback categories (technical, communication, leadership)
4. **Anonymous Feedback**: Option for anonymous feedback with manager visibility
5. **Feedback Requests**: Allow users to request feedback from peers
6. **Notifications**: Email/in-app notifications for new feedback
7. **Feedback Analytics**: Aggregate statistics for managers
8. **Feedback Templates**: Predefined templates for common scenarios

## Migration

Database migration: `V6__create_feedback_table.sql`

The migration is idempotent (uses `IF NOT EXISTS`) and can be safely re-run.

## Related Documentation

- [Permission System](permission-system.md)
- [Profile System](profile-system.md)
- [Authentication](authentication.md)
