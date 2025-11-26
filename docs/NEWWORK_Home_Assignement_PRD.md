# Product Requirements Document
## Employee Profile (HR Application) – Take-Home Assignment

**Product:** Employee Profile (HR application)  
**Version:** 1.2  
**Status:** Final  
**Owner:** Abdelrahman Deraz
**Last Updated:** 26 November 2025

---

## 1. Overview

This document defines a small Employee Profile application for the NEWWORK take-home assignment. The application is a single-page experience with a backend that supports:

- Role-aware profile visibility
- Peer feedback with optional AI-assisted polishing
- Absence requests and manager approval

Technical and architectural details (React, REST/GraphQL, backend stack, AI model) are described in the project README, not in this PRD.

---

## 2. Problem Statement (Assignment-Aligned)

The assignment requires building an Employee Profile system where:

- **The profile owner and their manager can:**
  - See all profile data
  - Change profile data (except system-managed fields)
  
- **A co-worker can:**
  - See only non-sensitive profile data
  - Leave feedback on that employee's profile
  - Optionally polish feedback through an AI service (HuggingFace)

- **An employee can:**
  - Request an absence

---

## 3. Roles & Capabilities

### 3.1 User Roles
- **Profile Owner** – the person whose profile is being viewed
- **Manager** – direct manager of the profile owner  
- **Co-worker** – other authenticated user in the system

### 3.2 Field Classifications
Employee profile data is grouped into three classification levels to define visibility, edit rights, and compliance requirements.

## 1. System-Managed Fields
HR/IT-controlled data. Always read-only in the product. Editable only through HR/IT workflows.

**Fields:**
- Employee ID (immutable)
- Legal first/last name (process-initiated)
- Work email (process-initiated)
- Manager ID (process-initiated)
- Department / Org unit (process-initiated)
- Job code / Job family (process-initiated)
- Job level / Grade (process-initiated)
- Employment status (process-initiated)
- Hire date (immutable)
- Termination date (immutable)
- Work schedule / FTE (process-initiated)

**Visibility:** Everyone  
**Edit:** HR/IT only (out of scope)

---

## 2. Non-Sensitive Fields
Information intended for internal visibility and collaboration.

**Fields:**
- Preferred/display name
- Job title (view-only)
- Office location
- Work phone
- Work location type (remote/hybrid/onsite)
- Bio / About
- Skills
- Profile photo

**Visibility:** Everyone  
**Edit:** Employee + Manager

---

## 3. Sensitive Fields
Private or regulated information with restricted visibility.

**Fields:**
- Personal email
- Personal phone
- Home address
- Emergency contacts
- Date of birth
- Visa / work permit
- Absence balance (process-initiated)
- Salary & compensation (process-initiated)
- Performance rating 

**Visibility:** Employee + Manager + HR  (out of scope)
**Edit:** Employee only  
(HR edits out of scope)

### 3.3 Permission Matrix

| Action / Field Type              | Employee (Owner) | Manager | Coworker |
|----------------------------------|------------------|---------|----------|
| **View – System-Managed**        | ✅               | ✅      | ✅       |
| **Edit – System-Managed**        | ❌               | ❌      | ❌       |
| **View – Non-Sensitive**         | ✅               | ✅      | ✅       |
| **Edit – Non-Sensitive**         | ✅               | ✅ *| ❌ |
| **View – Sensitive**             | ✅               | ✅ * | ❌ |
| **Edit – Sensitive**             | ✅               | ❌      | ❌       |
| **View – Compensation**          | ✅               | ✅ * | ❌ |
| **Edit – Compensation**          | ❌               | ❌      | ❌       |
| **View – Performance Rating**    | ✅               | ✅ * | ❌ |
| **Edit – Performance Rating**    | ❌               | ❌      | ❌       |
| **View – Absence Balance**       | ✅               | ✅ * | ❌ |
| **Edit – Absence Balance**       | ❌               | ❌      | ❌       |
| **Give Feedback**                | ✅               | ✅      | ✅       |
| **View Received Feedback**       | ✅               | ✅ * | ❌ |
| **Request Absence**              | ✅               | ✅      | ❌       |
| **Approve Absence**              | ❌               | ✅ * | ❌ |

*Only for direct reports

---

## 4. Feature Specifications

### 4.1 Profile Management

**Viewing**
- Relationship auto-detected: Self / Manager / Coworker.
- Field visibility follows classification + permission matrix.
- Sensitive fields hidden from coworkers.
- System-managed fields always read-only.

**Editing**
- Inline editing for editable fields.
- Validation on save (email, required fields, max length).
- Optimistic updates with rollback on error.
- Managers can edit non-sensitive fields for direct reports only.

---

### 4.2 Feedback System

**Visibility**
- Feedback visible to recipient and their manager.
- Coworkers cannot view feedback between others.
- Coworkers can view only the feedback they personally wrote.

**AI Enhancement**
1. Feedback text must be 10+ characters.
2. “✨ Polish with AI” option appears.
3. AI returns enhanced version.
4. Side-by-side comparison shown.
5. User chooses: Accept / Edit / Keep original.
6. Saved feedback includes AI-used flag.

---

### 4.3 Absence Management

**Request Workflow**
```
PENDING → APPROVED/REJECTED → COMPLETED
```
**Rules**
- Employee creates request (PENDING).
- Direct manager approves/rejects.
- Employees cannot approve their own requests.
- Coworkers have no access.
- Auto-transition to COMPLETED after end date.
- MVP notifications: in-app badges only.

**Request Fields**
- Start date (required)
- End date (required)
- Type (Vacation / Sick / Personal)
- Note (optional, max 200 chars)

### Acceptance Criteria

**Profile Management**
- [ ] Self sees all fields (system-managed, non-sensitive, sensitive).
- [ ] Manager sees: non-sensitive (edit), sensitive (view), system-managed (view).
- [ ] Coworker sees only non-sensitive + system-managed.
- [ ] Sensitive fields never appear in search, directory cards, or shared UI.
- [ ] Editing of system-managed fields is blocked.
- [ ] Validation errors appear inline.
- [ ] Optimistic updates rollback on error.

### Feedback System
- [ ] Feedback visible only to recipient + their manager.
- [ ] Coworkers cannot view feedback between others.
- [ ] Coworkers can view only feedback they personally wrote.
- [ ] AI enhancement available only when input is ≥ 10 characters.
- [ ] Side-by-side view shown before saving AI-enhanced text.
- [ ] Saved feedback includes AI-used flag.
- [ ] Feedback entries show author + timestamp.

### Absence Management
- [ ] Employees can submit valid absence requests with required fields.
- [ ] Managers can approve/reject only direct reports.
- [ ] Coworkers cannot view or act on requests.
- [ ] Status follows: PENDING → APPROVED/REJECTED → COMPLETED.
- [ ] COMPLETED status applies automatically after end date.
- [ ] Note field enforces 200-character limit.
- [ ] Dashboard badges show accurate pending counts and status updates.

---

## 5. Authentication Approach

**For MVP Simplicity:**
- Mock authentication with role switcher
- Three pre-seeded users (one of each role)
- JWT tokens with role claim
- No registration flow needed

**Demo Users:**
```javascript
[
  { id: 1, email: "john.doe@company.com", role: "employee", managerId: 2 },
  { id: 2, email: "jane.smith@company.com", role: "manager", managerId: null },
  { id: 3, email: "bob.wilson@company.com", role: "employee", managerId: 2 }
]
```

---

## 6. UI/UX Specifications

### 6.1 Single Page Layout

```
+----------------------------------------------+
| Header (User info, Role switch) |
+----------------------------------------------+
| | |
| Sidebar | Main Content Area |
| - Profile | (Dynamic based on |
| - Team | selected view) |
| - Pending | |
| | |
+----------------------------------------------+
```

### 6.2 Key Views

**Profile View**
- Sectioned info cards (system-managed, non-sensitive, sensitive)
- Inline edit mode per section
- Feedback list displayed at the bottom
- Prominent “Request Absence” action

**Team View (Manager Only)**
- List of direct reports with basic profile info
- Quick actions (view profile, give feedback)
- Badge showing count of pending absence requests

**Feedback Modal**
- Text area with live character count
- “✨ Polish with AI” button appears after 10+ characters
- Side-by-side preview of AI-enhanced text
- Submit and Cancel actions

---

## 7. Success Criteria

### Must Have (MVP)
- [ ] Role-based viewing works correctly
- [ ] Profile editing with field-level permissions
- [ ] Feedback creation with AI option
- [ ] Absence request/approval flow
- [ ] Mock auth with role switching

### Nice to Have
- [ ] Updates ((real-time)WebSocket/event drive)
- [ ] Profile photo upload
- [ ] Feedback sentiment analysis
- [ ] Absence calendar view

---

## 8. Implementation Priority

**Phase 1: Foundation**
- Database schema
- Authentication setup
- Basic profile CRUD with role checking
- Core API endpoints

**Phase 2: Feedback System**  
- Feedback creation flow
- HuggingFace integration
- Feedback visibility rules
- UI for feedback enhancement

**Phase 3: Absence Workflow**
- Request creation
- Manager approval interface  
- Status transitions
- Basic notifications (in-app)

**Phase 4: Polish**
- UI/UX improvements
- Error handling
- Edge cases (empty states, loading)
- Documentation

---

## 9. Edge Cases & Error Handling

### Key Scenarios
- AI service unavailable → Disable enhancement, allow regular feedback
- Manager changes → Existing feedback remains visible to new manager
- Self-reporting manager → Can request absence but cannot self-approve
- Empty states → Helpful messages for no feedback/no team members
- Validation failures → Clear error messages with recovery actions

---

## 10. Out of Scope (Acknowledged Limitations)

For this take-home assignment, the following are intentionally excluded:
- Email/Slack notifications
- Calendar integration
- Bulk operations
- Historical audit logs
- Password reset flow
- Data export/import
- Analytics dashboard
- Mobile app

These would be considered in a production system but are not needed to demonstrate core competencies.

---