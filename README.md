# Employee Profile Management System

A full-stack enterprise application for managing employee profiles, peer feedback, and absence requests with role-based permissions. Built for modern organizations to streamline HR operations and improve workplace transparency.

## What This Application Does

This system helps organizations manage their employees' information, enable peer-to-peer feedback, and handle time-off requests all in one place. Think of it as a central hub where employees can:
- Keep their work profile up to date
- Give and receive constructive feedback from teammates
- Request vacation or sick days
- Find and connect with coworkers

Managers get additional tools to review their team's feedback, approve time-off requests, and view direct reports.

## Tech Stack Overview

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React 19, TypeScript, Vite |
| **Backend** | Spring Boot 3.2, Java 17 |
| **Database** | PostgreSQL 15 |
| **APIs** | Hybrid REST + GraphQL |
| **Security** | JWT Authentication, BCrypt |
| **DevOps** | Docker, Docker Compose |
| **Migrations** | Flyway |

**Why these choices?** See [Architecture Decisions](docs/DESIGN_DECISIONS.md) for detailed rationale.

## Features (User Journey)

### 1. Authentication & Access
**For everyone**: Secure login with JWT tokens
- Employees, managers, and HR staff each get appropriate access levels
- Session management with 24-hour token expiration
- Password encryption with industry-standard BCrypt

### 2. View Your Profile
**For all employees**: See your complete employee information
- Personal details, job information, and contact data
- Employment history and current status
- Skills and bio for networking

### 3. Edit Your Profile
**For all employees**: Keep your information current
- Update contact information and emergency contacts
- Add or modify your bio and skill list
- Change your profile photo URL

### 4. View Coworker Profiles
**For all employees**: Find and learn about teammates
- Browse the company directory
- Search by name, email, or department
- See non-sensitive information about coworkers
- Managers see additional details about their direct reports

### 5. Give Feedback
**For all employees**: Provide constructive peer feedback
- Write feedback for any coworker
- Optional AI-powered text polishing for clarity
- Your identity is visible to recipient and their manager

### 6. Receive & View Feedback
**For all employees**: See feedback you've received
- View all feedback submitted about you
- Feedback visible to you and your manager
- Track growth and development over time

### 7. Request Time Off
**For all employees**: Submit absence requests
- Specify dates, type (vacation, sick, personal), and optional notes
- Track status (pending, approved, rejected, completed)
- View history of all your requests

### 8. Approve Absences (Managers Only)
**For managers**: Review team time-off requests
- See pending requests from direct reports
- Approve or reject with optional notes
- View team absence calendar

### 9. Review Team Feedback (Managers Only)
**For managers**: Monitor team development
- View feedback for your direct reports
- Track team growth and identify coaching opportunities
- Support performance review processes

### 10. Browse Direct Reports (Managers Only)
**For managers**: Access your team directory
- Filter directory to show only direct reports
- View detailed profiles of team members
- Access sensitive information when appropriate

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Ports 80, 8080, and 5432 available

### Start Everything
```bash
# Clone the repository
git clone https://github.com/3rabie/newwork-employee-profile.git
cd newwork-employee-profile

# Start all services
docker-compose up

# Access the application
# Frontend: http://localhost
# Backend API: http://localhost:8080
# GraphQL Playground: http://localhost:8080/graphiql
```
## Project Structure

```
newwork-employee-profile/
├── backend/              # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/    # Application code
│   │   │   └── resources/
│   │   │       ├── db/migration/  # Flyway database migrations
│   │   │       └── graphql/       # GraphQL schemas
│   │   └── test/        # Unit and integration tests
│   ├── Dockerfile       # Backend container image
│   └── README.md        # Backend documentation
│
├── frontend/            # React application
│   ├── src/
│   │   ├── features/    # Feature modules
│   │   │   ├── auth/    # Authentication
│   │   │   ├── profile/ # Profile management
│   │   │   ├── feedback/# Feedback system
│   │   │   ├── absence/ # Absence tracking
│   │   │   └── directory/ # Employee directory
│   │   └── lib/         # Shared utilities
│   ├── Dockerfile       # Frontend container image
│   └── README.md        # Frontend documentation
│
├── docs/                # Documentation
│   ├── DESIGN_DECISIONS.md  # Architecture rationale
│   ├── CODE_REVIEW_REPORT.md # Code review findings
│   └── features/        # Feature documentation
│
├── docker-compose.yml   # Full stack orchestration
└── README.md           # This file
```

## Documentation

### For Developers
- [Backend Documentation](backend/README.md) - API endpoints, database schema, testing
- [Frontend Documentation](frontend/README.md) - Component architecture, routing, state
- [Architecture Decisions](docs/DESIGN_DECISIONS.md) - Why we chose these technologies

### For Product/Business
- [Feature Documentation](docs/features/) - Detailed feature descriptions
- [NEWWORK Home Assignement PRD](docs/NEWWORK_Home_Assignement_PRD.md) - PRD

## Security & Production Readiness

This application implements enterprise-grade security:
- ✅ JWT-based stateless authentication
- ✅ BCrypt password hashing (10 rounds)
- ✅ Role-based access control (RBAC)
- ✅ Permission-based field filtering
- ✅ SQL injection prevention via JPA
- ✅ CORS configuration
- ✅ Secure secret management

**Before production deployment**:
1. Rotate JWT secret: `export JWT_SECRET=$(openssl rand -base64 32)`
2. Configure HuggingFace API key: `export APP_AI_HF_API_KEY=your-key`
3. Disable user switching: `export APP_SECURITY_SWITCH_USER_ENABLED=false`
4. Set strong database password
5. Review [Code Review Report](docs/CODE_REVIEW_REPORT.md) recommendations

## Development

### Local Development (without Docker)

**Backend**:
```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

**Frontend**:
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

**Database** (via Docker):
```bash
docker run -d --name employee_db \
  -e POSTGRES_DB=employee_profile \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

### Running Tests

**Backend**:
```bash
cd backend
mvn test                    # Unit tests
mvn verify                  # Integration tests (uses Testcontainers)
```

**Frontend**:
```bash
cd frontend
npm test                    # All tests
npm run test:watch          # Watch mode
```

## API Overview

### REST Endpoints (Mutations)
Used for creating and updating data:
- `POST /api/auth/login` - Authentication
- `POST /api/feedback` - Submit feedback
- `POST /api/absences` - Request time off
- `PATCH /api/profiles/{id}` - Update profile
- `PATCH /api/absences/{id}` - Approve/reject absence

### GraphQL Endpoint (Queries)
Used for reading data with flexible field selection:
- `POST /graphql` - All query operations
- GraphiQL Playground: http://localhost:8080/graphiql

**Why hybrid?** GraphQL solves N+1 query problems and over-fetching for reads, while REST keeps mutations simple and standard. See [Architecture Decisions](docs/DESIGN_DECISIONS.md#1-hybrid-rest--graphql-architecture).

## Performance Characteristics

- **Query Reduction**: GraphQL + DataLoader reduces database queries by 83-93%
- **Connection Pool**: HikariCP with 10 connections (configurable)
- **Lazy Loading**: JPA relationships load on-demand

## Known Limitations

- No audit log for profile changes (compliance requirement for some orgs)
- Synchronous AI feedback polish (blocks request, could be async)
- No pagination on large lists (directory, feedback)
- Single database instance (no read replicas or sharding)
- JWT cannot be revoked before expiration (24-hour window)
- no caching

See individual README files for component-specific limitations.
