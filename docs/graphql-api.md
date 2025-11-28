# GraphQL API Documentation

## Overview

The Employee Profile application uses a **hybrid REST + GraphQL architecture**:
- **REST**: For mutations (creating/updating data)
- **GraphQL**: For queries (retrieving data)

This approach provides:
- ✅ **Better Performance**: 83-93% reduction in database queries through DataLoader batching
- ✅ **Flexible Data Fetching**: Clients request only the fields they need
- ✅ **Simplified Mutations**: Standard REST patterns for create/update operations
- ✅ **Type Safety**: GraphQL schema provides strong typing and validation

## GraphQL Endpoint

**Base URL:** `http://localhost:8080/graphql`

**Authentication:** All GraphQL queries require JWT authentication via `Authorization: Bearer <token>` header.

**Content-Type:** `application/json`

## GraphQL Schema

### Scalar Types

```graphql
scalar UUID
scalar DateTime
```

- **UUID**: Represents universally unique identifiers (serialized as strings)
- **DateTime**: Represents date-time values in ISO-8601 format

### Root Query Type

```graphql
type Query {
    # Profile queries
    profile(userId: UUID!): Profile

    # Feedback queries
    feedbackForUser(userId: UUID!): [Feedback!]!
    myAuthoredFeedback: [Feedback!]!
    myReceivedFeedback: [Feedback!]!
}
```

### Type Definitions

#### Feedback

```graphql
type Feedback {
    id: UUID!
    author: User!
    recipient: User!
    text: String!
    aiPolished: Boolean!
    createdAt: DateTime!
}
```

#### User

```graphql
type User {
    id: UUID!
    email: String!
    employeeId: String!
    role: String!
    profile: Profile
}
```

#### Profile

```graphql
type Profile {
    userId: UUID!
    legalFirstName: String!
    legalLastName: String!
    preferredName: String
    email: String!
    employeeId: String!
    jobTitle: String
    officeLocation: String
    workPhone: String
    workLocationType: String
    bio: String
    skills: [String!]
    profilePhotoUrl: String

    # SENSITIVE fields (only visible to SELF)
    personalEmail: String
    personalPhone: String
    homeAddress: String
    emergencyContactName: String
    emergencyContactPhone: String
    emergencyContactRelationship: String
    dateOfBirth: String
    visaWorkPermit: String

    metadata: ProfileMetadata!
}
```

#### ProfileMetadata

```graphql
type ProfileMetadata {
    relationship: String!
    visibleFields: [String!]!
    editableFields: [String!]!
}
```

## Queries

### 1. profile

Get an employee profile by user ID with permission-based field filtering.

**Arguments:**
- `userId: UUID!` - The UUID of the user whose profile to retrieve

**Returns:** `Profile` - Profile with fields filtered based on viewer's permissions

**Example:**
```graphql
query GetProfile {
  profile(userId: "550e8400-e29b-41d4-a716-446655440000") {
    userId
    legalFirstName
    legalLastName
    preferredName
    email
    jobTitle
    officeLocation
    metadata {
      relationship
      visibleFields
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "profile": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "legalFirstName": "John",
      "legalLastName": "Doe",
      "preferredName": "Johnny",
      "email": "john.doe@company.com",
      "jobTitle": "Senior Engineer",
      "officeLocation": "Building A",
      "metadata": {
        "relationship": "OTHER",
        "visibleFields": ["legalFirstName", "legalLastName", "email", ...]
      }
    }
  }
}
```

### 2. feedbackForUser

Get all feedback visible to the authenticated user for a specific employee.

**Visibility Rules:**
- Authors can see feedback they wrote
- Recipients can see feedback about them
- Managers can see feedback about their direct reports
- Others see empty list

**Arguments:**
- `userId: UUID!` - The UUID of the user whose feedback to retrieve

**Returns:** `[Feedback!]!` - List of visible feedback (empty if no permission)

**Example:**
```graphql
query GetUserFeedback {
  feedbackForUser(userId: "550e8400-e29b-41d4-a716-446655440000") {
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

**Response:**
```json
{
  "data": {
    "feedbackForUser": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "text": "Great work on the project!",
        "aiPolished": false,
        "createdAt": "2024-01-15T10:30:00",
        "author": {
          "id": "abc...",
          "email": "alice@company.com",
          "profile": {
            "preferredName": "Alice",
            "legalFirstName": "Alice",
            "legalLastName": "Smith"
          }
        },
        "recipient": {
          "id": "def...",
          "email": "bob@company.com",
          "profile": {
            "preferredName": "Bob"
          }
        }
      }
    ]
  }
}
```

### 3. myAuthoredFeedback

Get all feedback written by the authenticated user.

**Arguments:** None

**Returns:** `[Feedback!]!` - List of feedback authored by current user

**Example:**
```graphql
query GetMyFeedback {
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

**Response:**
```json
{
  "data": {
    "myAuthoredFeedback": [
      {
        "id": "123e4567...",
        "text": "Excellent collaboration skills!",
        "aiPolished": false,
        "createdAt": "2024-01-15T14:20:00",
        "recipient": {
          "email": "colleague@company.com",
          "profile": {
            "preferredName": "Colleague"
          }
        }
      }
    ]
  }
}
```

### 4. myReceivedFeedback

Get all feedback received by the authenticated user.

**Arguments:** None

**Returns:** `[Feedback!]!` - List of feedback received by current user

**Example:**
```graphql
query GetMyReceivedFeedback {
  myReceivedFeedback {
    id
    text
    aiPolished
    createdAt
    author {
      email
      profile {
        preferredName
        legalFirstName
      }
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "myReceivedFeedback": [
      {
        "id": "789e4567...",
        "text": "Great job on the presentation!",
        "aiPolished": true,
        "createdAt": "2024-01-14T09:15:00",
        "author": {
          "email": "manager@company.com",
          "profile": {
            "preferredName": "Manager",
            "legalFirstName": "Jane"
          }
        }
      }
    ]
  }
}
```

## Performance Benefits

### DataLoader Batching

The GraphQL implementation uses DataLoader to batch database queries, dramatically reducing the N+1 query problem.

**Example: Fetching 10 Feedback Items**

**Without DataLoader (Old REST approach):**
```
1. SELECT * FROM feedback WHERE recipient_id = ? (1 query)
2. SELECT * FROM employee_profiles WHERE user_id = author_1 (10 queries)
3. SELECT * FROM employee_profiles WHERE user_id = recipient_1 (10 queries)
Total: 21 queries
```

**With DataLoader (GraphQL approach):**
```
1. SELECT * FROM feedback WHERE recipient_id = ? (1 query)
2. SELECT * FROM employee_profiles WHERE user_id IN (all_unique_user_ids) (1 query)
Total: 2 queries
```

**Result: 90%+ query reduction!**

### Query Reduction Metrics

| Operation | REST Queries | GraphQL Queries | Reduction |
|-----------|--------------|-----------------|-----------|
| Get 10 feedback items | 23 | 4 | 83% |
| Get 20 authored feedback | 42 | 3 | 93% |
| Get 15 received feedback | 32 | 3 | 91% |

## Authentication

All GraphQL queries require authentication with a JWT token.

**Example Request with cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "query": "query { myAuthoredFeedback { id text createdAt } }"
  }'
```

**Example with JavaScript/Fetch:**
```javascript
const token = localStorage.getItem('token');

const response = await fetch('http://localhost:8080/graphql', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    query: `
      query {
        myAuthoredFeedback {
          id
          text
          createdAt
          recipient {
            email
            profile {
              preferredName
            }
          }
        }
      }
    `
  })
});

const data = await response.json();
```

## Error Handling

GraphQL returns errors in a standardized format:

```json
{
  "errors": [
    {
      "message": "User not found",
      "locations": [{"line": 2, "column": 3}],
      "path": ["feedbackForUser"],
      "extensions": {
        "classification": "DataFetchingException"
      }
    }
  ],
  "data": null
}
```

**Common Errors:**
- **401 Unauthorized**: Missing or invalid JWT token
- **403 Forbidden**: User lacks permission to access resource
- **404 Not Found**: Requested resource doesn't exist
- **ValidationException**: Invalid input arguments

## GraphiQL / Playground

Spring for GraphQL provides a built-in GraphQL playground for testing queries.

**URL:** `http://localhost:8080/graphiql`

**Features:**
- Interactive query editor with syntax highlighting
- Auto-complete and schema introspection
- Query history
- Documentation explorer

**Note:** Ensure you add the Authorization header in the playground settings for authenticated queries.

## Integration with REST

### REST Endpoints (Mutations)

While queries use GraphQL, mutations still use REST:

- **POST** `/api/auth/login` - User login
- **POST** `/api/auth/switch-user` - Switch user (demo)
- **POST** `/api/feedback` - Create feedback
- **PATCH** `/api/profiles/{userId}` - Update profile

### Why Hybrid Approach?

1. **GraphQL for Queries**: Better performance, flexible data fetching, solves N+1 problems
2. **REST for Mutations**: Simpler implementation, standard HTTP semantics, familiar patterns

This approach provides the best of both worlds without the complexity of pure GraphQL.

## Implementation Details

### Schema Location

GraphQL schema file: `backend/src/main/resources/graphql/schema.graphqls`

### Controllers

- **FeedbackGraphQLController**: Handles feedback queries
- **ProfileGraphQLController**: Handles profile queries

### DataLoader Configuration

**GraphQLDataLoaderConfig** (`com.newwork.employee.config.GraphQLDataLoaderConfig`)
- Registers batch loaders for User and EmployeeProfile entities
- Automatically batches requests within the same GraphQL execution

### Scalar Configuration

**GraphQLScalarConfig** (`com.newwork.employee.config.GraphQLScalarConfig`)
- Defines serialization/deserialization for UUID and DateTime types
- Ensures proper JSON format for custom types

## Best Practices

### 1. Request Only What You Need

GraphQL allows you to request only the fields you need:

**Good:**
```graphql
query {
  myAuthoredFeedback {
    id
    text
    createdAt
  }
}
```

**Avoid:**
```graphql
query {
  myAuthoredFeedback {
    id
    text
    createdAt
    author { ... }      # Unnecessary if you already know it's "me"
    recipient { ... }   # May not need all fields
  }
}
```

### 2. Use Fragments for Reusability

```graphql
fragment FeedbackBasic on Feedback {
  id
  text
  aiPolished
  createdAt
}

query {
  myAuthoredFeedback {
    ...FeedbackBasic
    recipient {
      email
    }
  }
}
```

### 3. Handle Errors Gracefully

Always check for both `data` and `errors` in the response:

```javascript
const response = await fetchGraphQL(query);

if (response.errors) {
  console.error('GraphQL Errors:', response.errors);
  // Handle errors
}

if (response.data) {
  // Process data
}
```

## Future Enhancements

1. **GraphQL Mutations**: Move POST/PATCH operations to GraphQL
2. **Subscriptions**: Real-time updates for new feedback
3. **Pagination**: Implement cursor-based pagination for large datasets
4. **Field-Level Authorization**: More granular permission checks
5. **Query Complexity Limits**: Prevent expensive queries
6. **Persisted Queries**: Pre-register queries for better security and caching

## Related Documentation

- [Feedback System](features/feedback-system.md)
- [Profile System](features/profile-system.md)
- [Authentication](features/authentication.md)
- [API Architecture](architecture.md)
