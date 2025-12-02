# Employee Profile Backend

A Spring Boot application providing REST and GraphQL APIs for employee profile management with role-based permissions, peer feedback system, and absence tracking. Built with Spring Security (JWT), PostgreSQL, and Flyway for the NEWWORK take-home assignment.

## Table of Contents
- [Overview](#overview)
- [Permission Model](#permission-model)
- [API](#api)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Build & Run](#build--run)
- [Testing](#testing)
- [Docker](#docker)
- [Known Limitations](#known-limitations)

## Overview

The service implements three core features:
1. **Profile Management**: View and edit employee profiles with field-level permissions
2. **Feedback System**: Submit peer feedback with optional AI text polishing (HuggingFace)
3. **Absence Tracking**: Request time off and manager approval workflow

One GraphQL endpoint (`/graphql`) and multiple REST endpoints (`/api/*`) provide access to all features.

**Development Feature**: User switching is enabled by default. Add `?switch-user=email@company.com` to any request to instantly test different user roles without logging in/out. See [Configuration](#configuration) for details.

## Permission Model

Access control based on relationship between viewer and profile owner:

| Relationship | Can View | Can Edit |
|--------------|----------|----------|
| **SELF** | All fields | Non-sensitive + Sensitive |
| **MANAGER** | All fields | Non-sensitive + Sensitive |
| **COWORKER** | Non-sensitive only | Nothing |

Field classifications:
- **Non-sensitive**: name, job title, department, work email, office location, bio, skills
- **Sensitive**: personal email/phone, home address, emergency contacts, DOB, visa status
- **System**: id, created/updated timestamps (read-only)

## API

### Authentication
```bash
# Login
POST /api/auth/login
{
  "email": "alice.manager@company.com",
  "password": "password123"
}
# Returns: { "token": "eyJ...", "user": {...} }
```

### REST Endpoints
```bash
# Profile management
GET    /api/profiles/{userId}
PUT    /api/profiles/{userId}

# Feedback
POST   /api/feedback
POST   /api/feedback/polish
GET    /api/feedback?targetUserId={uuid}

# Absences
POST   /api/absences
GET    /api/absences/my-requests
GET    /api/absences/pending  # managers only
PATCH  /api/absences/{id}/approve
PATCH  /api/absences/{id}/reject

# Directory
GET    /api/directory?search=&department=&directReportsOnly=
```

### GraphQL

Endpoint: `http://localhost:8080/graphql`
Playground: `http://localhost:8080/graphiql`

```graphql
query {
  profile(userId: "uuid") {
    firstName lastName email
    metadata { relationship visibleFields editableFields }
  }

  coworkerDirectory(search: "john", department: "Engineering") {
    id fullName email jobTitle
  }

  myAbsenceRequests {
    id startDate endDate type status
  }
}
```

## Configuration

File: `src/main/resources/application.yml`

Key settings:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/employee_profile
    username: postgres
    password: postgres

jwt:
  secret: ${JWT_SECRET:default-secret-256-bits}
  expiration: 86400000  # 24 hours

app:
  ai:
    huggingface:
      api-key: ${APP_AI_HF_API_KEY}
      enabled: true
  security:
    switch-user-enabled: ${APP_SECURITY_SWITCH_USER_ENABLED:true}
```

**HuggingFace Setup** (for AI feedback polish):
1. Get token: https://huggingface.co/settings/tokens
2. Set environment: `export APP_AI_HF_API_KEY=hf_...`

**User Switching** (dev feature):
Add `?switch-user=email` to any request to impersonate that user. Enabled by default in development.

## Architecture

### Components
- **Controllers** (`controller/rest`, `controller/graphql`): HTTP/GraphQL endpoints
- **Services** (`service/impl`): Business logic and permission checks
- **Security** (`security/`): JWT authentication, user context
- **Repository** (`repository/`): Spring Data JPA interfaces
- **Entities** (`entity/`): JPA models (User, EmployeeProfile, Feedback, EmployeeAbsence)
- **DTOs** (`dto/`): API request/response objects
- **Mappers** (`mapper/`): Entity-DTO conversion (MapStruct)
- **Config** (`config/properties`, `config/jobs`): App configuration and scheduled tasks

### Request Flow
1. Controller receives request with JWT token
2. JwtAuthenticationFilter validates token and sets SecurityContext
3. Service determines viewer-profile relationship
4. PermissionService checks visibility/editability
5. Repository performs DB operations
6. Mapper converts entities to DTOs with filtered fields
7. Response returned with metadata showing permissions

### Resilience
- Connection pooling: HikariCP (default 10 connections)
- Database migrations: Flyway (auto-run on startup)
- Health monitoring: Spring Boot Actuator (`/actuator/health`)
- Error handling: Global exception handler with consistent JSON responses

### Database Schema
Managed by Flyway migrations (`src/main/resources/db/migration/`):
- V1: Users and authentication
- V2: Employee profiles
- V3: Demo seed data
- V4: Feedback system
- V5: Relationships and permissions
- V6: Absence workflow
- V7: Employee absences table

## Build & Run

Prerequisites: JDK 17+, Maven, PostgreSQL 15

### Option 1: Docker Compose (Recommended)
```bash
# From project root
docker-compose up

# Backend: http://localhost:8080
# Frontend: http://localhost:80
# Database: localhost:5432
```

### Option 2: Local Development
```bash
# Start PostgreSQL
docker run -d --name employee_profile_db \
  -e POSTGRES_DB=employee_profile \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15-alpine

# Build and run
mvn clean install
mvn spring-boot:run

# Or run JAR
java -jar target/employee-profile-0.0.1-SNAPSHOT.jar
```

### Test Users
| Email | Password | Role |
|-------|----------|------|
| alice.manager@company.com | password123 | Manager |
| bob.employee@company.com | password123 | Employee |
| charlie.hr@company.com | password123 | HR |

## Testing

```bash
# All tests
mvn test

# Integration tests only
mvn verify

# Specific test
mvn test -Dtest=ProfileServiceTest
```

Tests use:
- JUnit 5 + Mockito for unit tests
- Testcontainers for integration tests (PostgreSQL)
- @DataJpaTest for repository tests
- @WebMvcTest for controller tests

## Docker

Multi-stage Dockerfile for optimized production image:
```bash
# Build image
docker build -t employee-profile-backend .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db \
  -e JWT_SECRET=your-secret \
  employee-profile-backend
```

Health check included: `wget --quiet --tries=1 --spider http://localhost:8080/actuator/health`

## Known Limitations

- **No caching**: Profile queries hit database every time; consider Redis for high traffic
- **Single database**: No read replicas; write-heavy workloads may need sharding
- **Synchronous AI calls**: Feedback polish blocks request; could use async/queue
- **No audit log**: Profile changes not tracked; compliance may require event sourcing
- **Basic rate limiting**: Relies on JWT expiration; no per-user throttling
- **Demo user switching**: Insecure for production; must be disabled via config
