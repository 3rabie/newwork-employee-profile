# Employee Profile - Implementation Checklist

## Completed Features ✅

### Story 1.1.1: Project Setup ✅
- [x] Backend: Spring Boot 3.2.1 with Java 17
- [x] Frontend: React + Vite + TypeScript
- [x] Tests: N/A (infrastructure setup)
- [x] Documentation: README files

### Story 1.1.2: Database Setup & Flyway Configuration ✅
- [x] PostgreSQL 16 via Docker Compose
- [x] Flyway migrations enabled with performance optimizations
- [x] Idempotent seed data migrations
- [x] Tests: N/A (infrastructure)
- [x] Documentation: Covered in authentication.md

### Story 1.2.1: Authentication Backend Implementation ✅
- [x] Users table migration with manager hierarchy
- [x] User entity, Role enum, UserRepository
- [x] JWT token provider (jjwt 0.12.3)
- [x] Spring Security configuration with JWT filter
- [x] Auth service, controller, DTOs, mapper
- [x] Custom exceptions with global handler
- [x] OpenAPI/Swagger documentation
- [x] **Tests**: 39 tests total
  - [x] 17 unit tests (JwtTokenProviderTest)
  - [x] 8 unit tests (AuthServiceTest with Mockito)
  - [x] 14 integration tests (AuthControllerIntegrationTest with Testcontainers)
- [x] **Documentation**: [docs/features/authentication.md](../docs/features/authentication.md)

### Story 1.2.2: Frontend Authentication UI ✅
- [x] Login page with validation
- [x] Switch user dialog (demo feature)
- [x] JWT token storage (sessionStorage)
- [x] Axios interceptors for token attachment
- [x] Protected route wrapper
- [x] Auth context provider
- [x] Error handling and display
- [x] **Tests**: TODO - Frontend tests pending
- [x] **Documentation**: [docs/features/frontend-authentication.md](../docs/features/frontend-authentication.md)

---

### Story 1.2.3: Permission Service (Backend) ✅
- [x] Created enums: FieldType, Relationship
- [x] Created PermissionService interface and implementation
- [x] Implemented relationship detection (Self/Manager/Coworker)
- [x] Created permission evaluation methods (canView, canEdit)
- [x] **Tests**: 20 unit tests (all passing)
  - [x] Relationship detection tests (6 tests)
  - [x] View permission tests (6 tests)
  - [x] Edit permission tests (7 tests)
  - [x] Permission matrix verification (1 comprehensive test)
- [x] **Documentation**: [docs/features/permissions.md](features/permissions.md)

**Note**: Integration tests for permission-protected endpoints will be added when profile endpoints are implemented in Story 2.2.

---

## Remaining Work

### Phase 1: Complete Authentication & Profile Foundation

*All Phase 1 stories completed*

### Phase 2: Profile Management

#### Story 2.1: Employee Profile Entity & Repository ✅
- [x] Created EmployeeProfile entity with three field classifications
  - [x] SYSTEM_MANAGED fields (legal name, department, employment status, etc.)
  - [x] NON_SENSITIVE fields (preferred name, bio, skills, etc.)
  - [x] SENSITIVE fields (personal contact, salary, performance rating, etc.)
- [x] Created enums: EmploymentStatus, WorkLocationType
- [x] Created Flyway migration V4 for employee_profiles table
- [x] Created EmployeeProfileRepository with custom query methods
- [x] Created Flyway migration V5 to seed demo profiles (3 users)
- [x] **Tests**: 16 repository tests (all passing)
  - [x] CRUD operations tests
  - [x] Query method tests (findByUserId, findByDepartment, findByManagerId, etc.)
  - [x] Constraint validation tests (unique user_id, FTE validation)
  - [x] Cascade delete tests
  - [x] Field classification storage tests
- [x] **Documentation**: [docs/features/profile-management.md](features/profile-management.md)

#### Story 2.2: Profile API Endpoints ✅
**Tasks**:
- [x] GET /api/profiles/{userId} - View profile with permission filtering
- [x] PATCH /api/profiles/{userId} - Update editable fields with validation
- [x] ProfileDTO and ProfileUpdateDTO with validation annotations
- [x] ProfileMapper for entity-to-DTO conversions with permission filtering
- [x] ProfileService with business logic and permission checks
- [x] ProfileController with OpenAPI documentation
- [x] Exception handling (ResourceNotFoundException, ForbiddenException)
- [x] Global exception handler updates
- [x] **Tests**: 27 tests total (all passing)
  - [x] 13 service unit tests (ProfileServiceTest)
    - [x] Profile retrieval with different relationships (SELF, MANAGER, COWORKER)
    - [x] Profile updates with permission checks
    - [x] Exception handling (ResourceNotFoundException, ForbiddenException)
  - [x] 14 integration tests (ProfileControllerIntegrationTest)
    - [x] GET endpoint with field filtering by relationship
    - [x] PATCH endpoint with permission enforcement
    - [x] Validation error handling
    - [x] Authentication and authorization tests
- [x] **Documentation**: Updated [docs/features/profile-management.md](features/profile-management.md) with API specs

#### Story 2.3: Profile Frontend UI 🔲
**Tasks**:
- [ ] Profile view page with sectioned cards
- [ ] Inline edit mode for editable fields
- [ ] Field visibility based on user relationship
- [ ] Form validation and optimistic updates
- [ ] Error handling and rollback
- [ ] **Tests Required**:
  - [ ] Component tests (React Testing Library)
  - [ ] E2E tests for edit workflows
- [ ] **Documentation Required**:
  - [ ] Create `docs/features/frontend-profile.md`

### Phase 3: Feedback System

#### Story 3.1: Feedback Backend 🔲
**Tasks**:
- [ ] Create Feedback entity (author, recipient, text, AI flag, timestamp)
- [ ] Database migration for feedback table (idempotent)
- [ ] FeedbackRepository with visibility queries
- [ ] POST /api/feedback - Create feedback endpoint
- [ ] GET /api/profiles/{id}/feedback - List feedback with permissions
- [ ] **Tests Required**:
  - [ ] Unit tests for feedback service logic
  - [ ] Integration tests for feedback endpoints
  - [ ] Permission tests (visibility rules)
- [ ] **Documentation Required**:
  - [ ] Create `docs/features/feedback-system.md`

#### Story 3.2: AI Integration (HuggingFace) 🔲
**Tasks**:
- [ ] Add HuggingFace API client dependency
- [ ] Implement AIPolishService for text enhancement
- [ ] POST /api/feedback/polish endpoint (10+ character validation)
- [ ] Error handling for AI service unavailability
- [ ] **Tests Required**:
  - [ ] Unit tests with mocked AI service
  - [ ] Integration tests with fallback behavior
- [ ] **Documentation Required**:
  - [ ] Update `docs/features/feedback-system.md` with AI integration details

#### Story 3.3: Feedback Frontend 🔲
**Tasks**:
- [ ] Feedback modal with text area
- [ ] "✨ Polish with AI" button (appears at 10+ chars)
- [ ] Side-by-side comparison view
- [ ] Accept/Edit/Keep original actions
- [ ] Feedback list display on profile
- [ ] **Tests Required**:
  - [ ] Component tests for feedback modal
  - [ ] Integration tests for AI polish flow
- [ ] **Documentation Required**:
  - [ ] Create `docs/features/frontend-feedback.md`

### Phase 4: Absence Management

#### Story 4.1: Absence Backend 🔲
**Tasks**:
- [ ] Create AbsenceRequest entity (start/end date, type, status, note)
- [ ] Database migration for absence_requests table (idempotent)
- [ ] AbsenceRepository with manager queries
- [ ] POST /api/absence - Create request endpoint
- [ ] GET /api/absence/pending - Pending requests for manager
- [ ] PATCH /api/absence/{id}/approve - Approve endpoint (manager only)
- [ ] PATCH /api/absence/{id}/reject - Reject endpoint (manager only)
- [ ] Scheduled job for auto-COMPLETED after end date
- [ ] **Tests Required**:
  - [ ] Unit tests for absence service logic
  - [ ] Integration tests for all endpoints
  - [ ] Scheduled job tests
  - [ ] Permission tests (manager-only actions)
- [ ] **Documentation Required**:
  - [ ] Create `docs/features/absence-management.md`

#### Story 4.2: Absence Frontend 🔲
**Tasks**:
- [ ] "Request Absence" form on profile
- [ ] Absence type dropdown (Vacation/Sick/Personal)
- [ ] Date range picker with validation
- [ ] Note field (200 char limit)
- [ ] Manager approval interface (Team View)
- [ ] Status badges and pending count
- [ ] **Tests Required**:
  - [ ] Component tests for absence forms
  - [ ] E2E tests for approval workflow
- [ ] **Documentation Required**:
  - [ ] Create `docs/features/frontend-absence.md`

### Phase 5: Manager Features

#### Story 5.1: Team View 🔲
**Tasks**:
- [ ] GET /api/team/reports - List direct reports endpoint
- [ ] Team view page (manager only)
- [ ] Direct reports list with profile cards
- [ ] Quick actions: view profile, give feedback
- [ ] Pending absence request badge
- [ ] **Tests Required**:
  - [ ] Integration tests for team endpoints
  - [ ] Permission tests (manager-only access)
  - [ ] Component tests for team view
- [ ] **Documentation Required**:
  - [ ] Create `docs/features/team-management.md`

### Phase 6: Polish & Production Readiness

#### Story 6.1: Error Handling 🔲
**Tasks**:
- [ ] Verify all custom exception classes exist
- [ ] Ensure global exception handler covers all cases
- [ ] Frontend error boundary component
- [ ] User-friendly error messages
- [ ] **Tests Required**:
  - [ ] Error handling tests for all endpoints
  - [ ] Error boundary tests
- [ ] **Documentation Required**:
  - [ ] Update existing feature docs with error handling sections

#### Story 6.2: Comprehensive Testing 🔲
**Tasks**:
- [ ] Backend integration tests for all features
- [ ] Frontend unit tests for components
- [ ] E2E tests for critical user flows
- [ ] Test coverage report (aim for >80%)
- [ ] **Tests Required**: This IS the testing story
- [ ] **Documentation Required**:
  - [ ] Create `docs/TESTING.md` with testing guidelines

#### Story 6.3: Documentation Completion 🔲
**Tasks**:
- [ ] Verify all features have documentation
- [ ] Update main README with feature overview
- [ ] Add setup/deployment instructions
- [ ] Create API documentation compilation
- [ ] **Tests Required**: N/A
- [ ] **Documentation Required**: This IS the documentation story

#### Story 6.4: Nice-to-Have Features (Optional) 🔲
**Tasks**:
- [ ] Real-time updates (WebSocket/SSE)
- [ ] Profile photo upload
- [ ] Feedback sentiment analysis
- [ ] Absence calendar view
- [ ] **Tests Required**: Per feature
- [ ] **Documentation Required**: Per feature

---

## Testing & Documentation Standards

### For Every Story/Task:

1. **Testing Requirements**:
   - Unit tests for business logic (services, utilities)
   - Integration tests for API endpoints (with Testcontainers)
   - Component tests for UI components (React Testing Library)
   - E2E tests for critical user workflows (optional but recommended)
   - Minimum 70% code coverage for new code

2. **Documentation Requirements**:
   - Feature documentation in `docs/features/`
   - Concise but rich with context
   - Include: overview, architecture, API specs, testing instructions
   - Design decisions explained (why, not just what)
   - Extension points for future development
   - Troubleshooting section

3. **Definition of Done**:
   - [ ] Code implemented and reviewed
   - [ ] All tests passing
   - [ ] Test coverage meets minimum threshold
   - [ ] Documentation created/updated
   - [ ] Manual testing completed
   - [ ] Git commit following conventional commits format

---

## Progress Summary

**Total Stories**: 19
**Completed**: 7 (37%)
**Remaining**: 12 (63%)

**Test Coverage**:
- Backend: 102 tests implemented
  - Authentication: 39 tests
  - Permissions: 20 tests
  - Profile Repository: 16 tests
  - Profile Service: 13 tests
  - Profile Integration: 14 tests
- Frontend: 0 tests implemented (pending)

**Documentation**:
- Backend auth: Complete
- Frontend auth: Complete
- User switching: Complete
- Permissions: Complete
- Profile management: Complete (Entity + API)
- Other features: Pending
