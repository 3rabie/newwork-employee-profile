# Employee Profile Frontend

A React application providing employee profile management with role-based permissions, peer feedback, absence tracking, and directory search. Built with React 19, TypeScript, Vite, and React Router for the NEWWORK take-home assignment.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Build & Run](#build--run)
- [Testing](#testing)
- [Docker](#docker)
- [Known Limitations](#known-limitations)

## Overview

The frontend provides four core features:
1. **Profile Management**: View and edit employee profiles with field-level permission controls
2. **Feedback System**: Submit peer feedback with optional AI text polishing (HuggingFace)
3. **Absence Tracking**: Request time off and manage approval workflow
4. **Employee Directory**: Search and browse coworkers with filters

Two API protocols supported: REST (Axios) for mutations, GraphQL (graphql-request) for queries.

## Features

### Permission-Based UI
Fields are shown/hidden based on viewer relationship:
- **Self/Manager**: See all fields including sensitive data (personal email, phone, address, emergency contacts)
- **Coworker**: See only non-sensitive fields (work email, job title, department, bio)
- Edit controls disabled when viewer lacks permission

### User Switching (Development)
Add `?switch-user=email` parameter to test different roles instantly. Controlled by `VITE_ENABLE_SWITCH_USER` environment variable (enabled by default).

### AI Feedback Polish
Optional checkbox when submitting feedback sends text to HuggingFace API for grammar/tone improvements. Requires backend API key configuration (see [Backend README](../backend/README.md#configuration)).

### Absence Workflow
- Employees: Create requests, view status
- Managers: Approve/reject team member requests, view pending queue

## Configuration

File: `.env` at frontend root

Key settings:
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_ENABLE_SWITCH_USER=true
```

**Note**: Vite embeds environment variables at build time. Rebuild after changes.

**Default Test Users**:
| Email | Password | Role |
|-------|----------|------|
| alice.manager@company.com | password123 | Manager |
| bob.employee@company.com | password123 | Employee |
| charlie.hr@company.com | password123 | HR |

## Architecture

### Tech Stack
- React 19 + React Router 7
- TypeScript 5.9
- Vite 7 (build tool)
- Axios (REST API)
- graphql-request (GraphQL API)
- Vitest + Testing Library (tests)

### Project Structure
```
frontend/
├── src/
│   ├── features/            # Feature modules
│   │   ├── auth/            # Login, protected routes, context
│   │   ├── profile/         # Profile viewing/editing
│   │   ├── feedback/        # Feedback submission/list
│   │   ├── absence/         # Absence requests/approvals
│   │   └── directory/       # Employee search/browse
│   ├── lib/                 # Shared utilities
│   │   ├── http-client.ts   # Axios instance with JWT
│   │   ├── graphql-client.ts # GraphQL client
│   │   └── graphql-queries.ts # Query definitions
│   ├── components/          # Shared components
│   ├── config.ts            # Environment config
│   ├── App.tsx              # Routes and providers
│   └── main.tsx             # Entry point
└── Dockerfile               # Production build
```

### Authentication Flow
1. User logs in via [LoginPage.tsx](src/features/auth/pages/LoginPage.tsx)
2. JWT token stored in localStorage
3. AuthContext provides token to all components
4. http-client.ts adds `Authorization: Bearer <token>` to requests
5. ProtectedRoute redirects unauthenticated users to login

### API Communication
- **REST**: Authentication, mutations (feedback, absences)
- **GraphQL**: Queries (profiles, directory, absence lists)
- User switching: Adds `switch-user` param to URL when enabled

## Build & Run

Prerequisites: Node.js 18+

### Option 1: Docker Compose (Recommended)
```bash
# From project root
docker-compose up

# Frontend: http://localhost (port 80)
# Backend: http://localhost:8080
# Database: localhost:5432
```

### Option 2: Local Development
```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Available at: http://localhost:5173
```

**Prerequisites**: Backend running on localhost:8080

### Build Production Bundle
```bash
npm run build
# Output: dist/ directory
```

## Testing

```bash
# All tests
npm test

# Watch mode
npm run test:watch

# With UI
npm run test:ui

# Specific test
npm test DirectoryPage.test
```

Tests use Vitest + Testing Library. Example structure:
```typescript
import { render, screen } from '@testing-library/react';
import { DirectoryPage } from './DirectoryPage';

test('renders search input', () => {
  render(<DirectoryPage />);
  expect(screen.getByPlaceholderText('Search')).toBeInTheDocument();
});
```

## Docker

Multi-stage Dockerfile for optimized production image:
```bash
# Build image
docker build -t employee-profile-frontend .

# Run container
docker run -p 80:80 \
  -e VITE_API_BASE_URL=http://localhost:8080 \
  employee-profile-frontend
```

Production deployment uses Nginx with:
- Gzip compression
- Static asset caching (1 year for hashed assets)
- Client-side routing support (fallback to index.html)
- Security headers (X-Frame-Options, X-Content-Type-Options)

## Known Limitations

- **No optimistic updates**: UI waits for server confirmation; could use React Query for instant feedback
- **No caching layer**: Profile queries re-fetch on mount; consider SWR/React Query
- **JWT in localStorage**: Vulnerable to XSS; httpOnly cookies would be more secure
- **No retry logic**: Failed requests don't auto-retry; network resilience could improve
- **Single GraphQL endpoint**: All queries go to `/graphql`; could split by domain for scaling
- **Dev user switching**: Insecure for production; must disable via `VITE_ENABLE_SWITCH_USER=false`
