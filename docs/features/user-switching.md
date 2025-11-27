# User Switching Feature

## Overview

The User Switching feature is a **development and testing utility** that allows you to quickly switch between different user accounts without needing to log out and re-authenticate with passwords. This is particularly useful during development when testing role-based features and permissions.

## Purpose and Use Cases

### Primary Purpose
Enable rapid context switching between different user roles (EMPLOYEE and MANAGER) during development and testing without the overhead of full authentication.

### Key Use Cases

1. **Role-Based Feature Testing**
   - Quickly test how features appear differently for EMPLOYEE vs MANAGER roles
   - Verify permission controls and access restrictions
   - Test UI/UX variations based on user roles

2. **Development Workflow Efficiency**
   - Eliminate the need to remember and enter multiple passwords during development
   - Speed up testing cycles by removing authentication friction
   - Allow developers to rapidly switch contexts while debugging

3. **Demo and Presentation**
   - Seamlessly demonstrate different user perspectives during product demos
   - Show role-specific features without interrupting the presentation flow
   - Quickly showcase manager oversight vs employee self-service capabilities

4. **QA and Testing**
   - Enable testers to rapidly verify user stories from different role perspectives
   - Simplify end-to-end testing scenarios that involve multiple users
   - Facilitate exploratory testing across different user contexts

## How It Works

### Architecture

The user switching feature consists of three main components:

#### 1. Backend API Endpoint
**Location:** [AuthController.java:100](backend/src/main/java/com/newwork/employee/controller/AuthController.java#L100)

```
POST /api/auth/switch-user
Content-Type: application/json

{
  "email": "user@company.com"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "employeeId": 101,
  "email": "user@company.com",
  "role": "EMPLOYEE",
  "managerId": 100
}
```

#### 2. Backend Service Logic
**Location:** [AuthServiceImpl.java:66](backend/src/main/java/com/newwork/employee/service/AuthServiceImpl.java#L66)

The `switchUser()` method:
- Looks up the user by email
- Bypasses password authentication entirely
- Generates a fresh JWT token for the target user
- Returns complete user profile information

#### 3. Frontend Components

**Switch User Dialog:** [SwitchUserDialog.tsx](frontend/src/features/auth/components/SwitchUserDialog.tsx)
- Modal dialog with email input field
- Quick-switch buttons for demo accounts (Manager, Employee 1, Employee 2)
- Email validation and error handling

**Integration:** [HomePage.tsx:22](frontend/src/pages/HomePage.tsx#L22)
- "Switch User" button in the application header
- Opens the switch user dialog on click
- Updates the entire application context upon successful switch

## Security Considerations

### ⚠️ IMPORTANT: This is a DEMO Feature Only

**NEVER deploy this feature to production environments.**

### Why This is NOT Production-Safe

1. **No Authentication Required**
   - Anyone can become any user by simply knowing their email
   - Completely bypasses password verification
   - No authorization checks whatsoever

2. **Security Vulnerabilities**
   - Enables unauthorized access to any account
   - Violates principle of least privilege
   - Creates audit trail gaps (actions performed as other users)
   - Potential for data breach and privacy violations

3. **Compliance Issues**
   - Violates most security compliance standards (SOC 2, ISO 27001, etc.)
   - May breach data protection regulations (GDPR, CCPA, etc.)
   - Creates liability for unauthorized data access

### Recommended Safeguards

If you must use this feature beyond local development:

1. **Environment Restriction**
   ```java
   @Profile("dev")  // Only enable in development profile
   ```

2. **Feature Flags**
   - Disable via environment variables in non-dev environments
   - Use configuration management to ensure it's never active in production

3. **Remove Before Production**
   - Delete the endpoint entirely before production deployment
   - Remove UI components and related code
   - Consider it temporary scaffolding, not a feature

## Available Demo Users

The application comes pre-seeded with three demo users for testing:

| Email | Role | Employee ID | Manager ID | Password |
|-------|------|-------------|------------|----------|
| manager@company.com | MANAGER | 100 | null | pwd951753 |
| emp1@company.com | EMPLOYEE | 101 | 100 | pwd951753 |
| emp2@company.com | EMPLOYEE | 102 | 100 | pwd951753 |

### Quick Switch Buttons

The Switch User Dialog provides quick-switch buttons for these accounts:
- **Manager** → manager@company.com
- **Employee 1** → emp1@company.com
- **Employee 2** → emp2@company.com

## Usage Guide

### From the UI

1. Log in to the application with any account
2. Click the **"Switch User"** button in the header
3. Either:
   - Click a quick-switch button (Manager, Employee 1, Employee 2), or
   - Enter any user email manually
4. Click **"Switch User"** to confirm
5. The application will immediately switch to the new user context

### From the API

Using curl:
```bash
curl -X POST http://localhost:8080/api/auth/switch-user \
  -H "Content-Type: application/json" \
  -d '{"email": "manager@company.com"}'
```

Using JavaScript/TypeScript:
```typescript
const response = await fetch('/api/auth/switch-user', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'emp1@company.com' })
});

const { token, ...user } = await response.json();
// Store token and update application state
```

## Implementation Details

### Frontend Flow

1. User clicks "Switch User" button
2. [SwitchUserDialog](frontend/src/features/auth/components/SwitchUserDialog.tsx) modal opens
3. User selects or enters target user email
4. Dialog calls `switchUser()` from [AuthContext](frontend/src/features/auth/contexts/AuthContext.tsx)
5. AuthContext makes API call to `/api/auth/switch-user`
6. On success, updates local state with new token and user info
7. Dialog closes, app re-renders with new user context

### Backend Flow

1. Request received at [AuthController:100](backend/src/main/java/com/newwork/employee/controller/AuthController.java#L100)
2. Email validated (format check only)
3. [AuthServiceImpl:66](backend/src/main/java/com/newwork/employee/service/AuthServiceImpl.java#L66) looks up user by email
4. If user found, generates fresh JWT token
5. Returns AuthResponse with token and user details
6. If user not found, throws 404 UserNotFoundException

### Data Seeding

Demo users are seeded via Flyway migration:
- **Location:** [V3__seed_demo_users.sql](backend/src/main/resources/db/migration/V3__seed_demo_users.sql)
- Passwords are BCrypt hashed with strength 10
- All demo users share the password: `pwd951753`

## Testing the Feature

### Manual Testing Scenarios

1. **Basic Switch**
   - Log in as emp1@company.com
   - Switch to manager@company.com
   - Verify role badge changes from EMPLOYEE to MANAGER
   - Verify manager-specific features become visible

2. **Quick Switch Buttons**
   - Test each quick-switch button
   - Verify email field populates correctly
   - Confirm switch completes successfully

3. **Error Handling**
   - Try switching to non-existent email (should show error)
   - Try invalid email format (should show validation error)
   - Cancel dialog (should close without switching)

4. **Session Continuity**
   - Switch users multiple times in succession
   - Verify new JWT token is properly stored and used
   - Confirm authentication persists across switches

### Automated Tests

Backend integration tests are available at:
- [AuthControllerIntegrationTest.java](backend/src/test/java/com/newwork/employee/controller/AuthControllerIntegrationTest.java)
- [AuthServiceTest.java](backend/src/test/java/com/newwork/employee/service/AuthServiceTest.java)

## Removing This Feature

When preparing for production deployment:

### Backend Cleanup

1. Delete the switch-user endpoint:
   - Remove `switchUser()` method from [AuthController.java](backend/src/main/java/com/newwork/employee/controller/AuthController.java)
   - Remove `switchUser()` from [AuthService.java](backend/src/main/java/com/newwork/employee/service/AuthService.java)
   - Remove implementation from [AuthServiceImpl.java](backend/src/main/java/com/newwork/employee/service/AuthServiceImpl.java)

2. Delete related DTOs:
   - Remove [SwitchUserRequest.java](backend/src/main/java/com/newwork/employee/dto/request/SwitchUserRequest.java)

3. Remove related tests

### Frontend Cleanup

1. Delete components:
   - Remove [SwitchUserDialog.tsx](frontend/src/features/auth/components/SwitchUserDialog.tsx)
   - Remove [SwitchUserDialog.css](frontend/src/features/auth/components/SwitchUserDialog.css)

2. Update [HomePage.tsx](frontend/src/pages/HomePage.tsx):
   - Remove "Switch User" button
   - Remove dialog state and imports

3. Update [AuthContext.tsx](frontend/src/features/auth/contexts/AuthContext.tsx):
   - Remove `switchUser()` function
   - Remove from context interface

4. Update [auth-service.ts](frontend/src/features/auth/api/auth-service.ts):
   - Remove `switchUser()` API function

## Alternatives for Production

For production environments, consider these alternatives:

### 1. Admin Impersonation (with Audit Trail)
- Require admin privileges to impersonate
- Log all impersonation events
- Display clear indicators when impersonating
- Auto-timeout impersonation sessions
- Require password re-authentication

### 2. Role-Based Test Accounts
- Maintain separate test accounts for each role
- Require normal login for all accounts
- No special switching privileges
- Use standard authentication flow

### 3. Development-Only Feature Flag
- Keep the feature but guard it with environment checks
- Completely disable in production builds
- Use Spring profiles: `@Profile("dev")`

## Summary

The User Switching feature is a powerful development tool that significantly speeds up testing and development workflows by eliminating authentication friction. However, it **must be removed or properly secured before production deployment** due to its inherent security risks. Use it liberally during development, but treat it as temporary scaffolding rather than a permanent feature.
