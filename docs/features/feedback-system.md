# Feedback System

## Overview

The Feedback System allows employees to give and receive peer feedback. This feature implements privacy controls ensuring that feedback is only visible to authorized users.

**Architecture:** Hybrid REST + GraphQL
- **REST**: For mutations (creating feedback)
- **GraphQL**: For queries (retrieving feedback) - provides better performance with 83-93% query reduction

## Features

- **Create Feedback**: Employees can give feedback to their peers
- **View Feedback**: Users can view feedback based on their relationship (author, recipient, or manager)
- **Privacy Controls**: Strict visibility rules ensure feedback privacy
- **AI Polish Flag**: Optional flag to indicate AI-enhanced feedback
- **Efficient Queries**: GraphQL with DataLoader prevents N+1 query problems

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
JOIN f.recipient r
LEFT JOIN r.manager m
WHERE r.id = :userId
AND (
    f.author.id = :viewerId           -- Viewer is author
    OR r.id = :viewerId                -- Viewer is recipient
    OR m.id = :viewerId                -- Viewer is recipient's manager
)
ORDER BY f.createdAt DESC
```

## API Endpoints

### REST API (Mutations)

#### POST /api/feedback

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

### GraphQL API (Queries)

All query operations have been moved to GraphQL for better performance. See [GraphQL API Documentation](../graphql-api.md) for details.

#### Query: feedbackForUser

Get all feedback visible to the authenticated user for a specific profile.

**Authentication:** Required

**Query:**
```graphql
query {
  feedbackForUser(userId: "uuid-here") {
    id
    text
    aiPolished
    createdAt
    author {
      id
      email
      profile {
        preferredName
        legalFirstName
        legalLastName
      }
    }
    recipient {
      id
      email
      profile {
        preferredName
      }
    }
  }
}
```

**Visibility:** Returns only feedback visible based on viewer's relationship (author, recipient, or manager)

#### Query: myAuthoredFeedback

Get all feedback written by the authenticated user.

**Authentication:** Required

**Query:**
```graphql
query {
  myAuthoredFeedback {
    id
    text
    aiPolished
    createdAt
    recipient {
      email
      profile {
        preferredName
      }
    }
  }
}
```

#### Query: myReceivedFeedback

Get all feedback received by the authenticated user.

**Authentication:** Required

**Query:**
```graphql
query {
  myReceivedFeedback {
    id
    text
    aiPolished
    createdAt
    author {
      email
      profile {
        preferredName
      }
    }
  }
}
```

## Performance Optimization

### N+1 Query Problem Solution

The feedback system previously suffered from N+1 query problems:

**Before (REST):**
- Get 10 feedback items: **23 database queries**
  - 1 query: Fetch feedback
  - 10 queries: Fetch author profiles
  - 10 queries: Fetch recipient profiles

**After (GraphQL with DataLoader):**
- Get 10 feedback items: **4 database queries** (83% reduction)
  - 1 query: Fetch feedback
  - 1 query: Batch load all unique user profiles

### Query Reduction Results

| Scenario | REST Queries | GraphQL Queries | Reduction |
|----------|--------------|-----------------|-----------|
| Get 10 feedback items | 23 | 4 | **83%** ↓ |
| Get 20 authored feedback | 42 | 3 | **93%** ↓ |
| Get 15 received feedback | 32 | 3 | **91%** ↓ |

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
- REST endpoint for creating feedback (POST)
- Uses `@AuthenticationPrincipal` for authenticated user
- Input validation with `@Valid`
- Returns appropriate HTTP status codes

**FeedbackGraphQLController** (`com.newwork.employee.graphql.FeedbackGraphQLController`)
- GraphQL queries for retrieving feedback
- Schema mappings for nested User and Profile resolution
- Integrates with DataLoader for batch fetching

### GraphQL DataLoader

**GraphQLDataLoaderConfig** (`com.newwork.employee.config.GraphQLDataLoaderConfig`)
- Batch loader for User entities
- Batch loader for EmployeeProfile entities
- Automatically batches multiple profile fetches into single queries

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
- GraphQL queries respect the same authentication and authorization rules as REST

## Testing

### Unit Tests

**FeedbackServiceTest** (`com.newwork.employee.service.FeedbackServiceTest`)
- Tests all service methods with mocked dependencies
- Validates error handling (self-feedback, missing users)
- Verifies visibility logic
- Uses JUnit 5 with Mockito
- 13 tests covering create, authored, and received feedback scenarios

### Integration Tests

**FeedbackControllerIntegrationTest** (`com.newwork.employee.controller.FeedbackControllerIntegrationTest`)
- Full-stack tests with Testcontainers PostgreSQL
- Tests REST endpoints with real authentication
- Validates visibility rules with multiple users
- Verifies authorization and permission logic
- 11 tests covering CRUD operations and edge cases

**Note:** GraphQL endpoint tests to be added in future iteration.

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
9. **GraphQL Mutations**: Move POST endpoint to GraphQL for consistency
10. **GraphQL Subscriptions**: Real-time feedback updates

## Migration

Database migration: `V6__create_feedback_table.sql`

The migration is idempotent (uses `IF NOT EXISTS`) and can be safely re-run.

## Related Documentation

- [GraphQL API](../graphql-api.md)
- [Permission System](permission-system.md)
- [Profile System](profile-system.md)
- [Authentication](authentication.md)
