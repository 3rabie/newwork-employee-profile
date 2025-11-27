# Frontend Profile UI Documentation

**Story 2.3: Profile Frontend UI**

## Overview

This document describes the React TypeScript implementation of the employee profile viewing and editing interface. The implementation follows Story 2.3 requirements and integrates with the Profile API endpoints from Story 2.2.

## File Structure

```
frontend/src/features/profile/
├── types.ts                          # TypeScript types and enums
├── api/
│   └── profileApi.ts                 # API service functions
├── components/
│   ├── ProfileField.tsx              # Individual field display/edit
│   └── ProfileSection.tsx            # Grouped field sections
└── pages/
    ├── ProfilePage.tsx               # Main profile page
    └── ProfilePage.css               # Profile page styles
```

## Components

### 1. ProfilePage

**Location:** `frontend/src/features/profile/pages/ProfilePage.tsx`

Main page component for viewing and editing employee profiles.

**Features:**
- Fetches profile data from `/api/profiles/{userId}` on mount
- Supports view and edit modes with toggle button
- Permission-based field visibility (handled by backend)
- Inline editing with save/cancel actions
- Error handling for fetch and update operations
- Loading states during async operations

**Props:**
- Uses React Router `useParams` to get `userId` from URL
- Accesses current user via `useAuth` context

**State:**
- `profile`: Current profile data from backend
- `editedProfile`: Partial updates in edit mode
- `isEditMode`: Boolean flag for edit mode
- `loading`: Loading state for initial fetch
- `saving`: Saving state during update
- `error`: Error message if operations fail

**Key Behaviors:**
- Only shows "Edit Profile" button when viewing own profile (`isSelf`)
- Merges `editedProfile` changes into display for real-time preview
- Sends only changed fields to backend on save
- Cancels edit mode and discards changes on cancel
- Displays three sections: Basic Information, Work Details, Personal & Confidential

### 2. ProfileSection

**Location:** `frontend/src/features/profile/components/ProfileSection.tsx`

Groups related profile fields into titled sections.

**Props:**
- `title`: Section heading text
- `fields`: Array of field metadata to display
- `profile`: Current profile data
- `isEditMode`: Whether section is in edit mode
- `onChange`: Callback for field value changes

**Features:**
- Filters out undefined fields (fields without values)
- Renders grid layout of ProfileField components
- Returns null if no visible fields

### 3. ProfileField

**Location:** `frontend/src/features/profile/components/ProfileField.tsx`

Reusable component for individual profile fields.

**Props:**
- `metadata`: Field configuration (label, type, editability)
- `value`: Current field value
- `isEditMode`: Whether field is editable
- `onChange`: Callback for value changes

**Field Types:**
- `text`: Standard text input
- `email`: Email input with validation
- `tel`: Phone number input
- `textarea`: Multi-line text area
- `date`: Date picker
- `number`: Numeric input with decimal support
- `select`: Dropdown for enums (WorkLocationType, EmploymentStatus)

**View Mode:**
- Displays field label and value
- Formats dates as locale strings
- Shows "N/A" for empty values

**Edit Mode:**
- Only renders input if field is editable
- Renders appropriate input type based on metadata
- Handles change events and passes updates to parent

## Types

### TypeScript Types

**Location:** `frontend/src/features/profile/types.ts`

#### Enums

```typescript
enum EmploymentStatus {
  ACTIVE = 'ACTIVE',
  ON_LEAVE = 'ON_LEAVE',
  TERMINATED = 'TERMINATED'
}

enum WorkLocationType {
  OFFICE = 'OFFICE',
  REMOTE = 'REMOTE',
  HYBRID = 'HYBRID'
}

enum Relationship {
  SELF = 'SELF',
  MANAGER = 'MANAGER',
  COWORKER = 'COWORKER'
}

enum FieldType {
  SYSTEM_MANAGED = 'SYSTEM_MANAGED',
  NON_SENSITIVE = 'NON_SENSITIVE',
  SENSITIVE = 'SENSITIVE'
}
```

#### Interfaces

**ProfileDTO:**
- Matches backend `ProfileDTO` structure
- All fields except `id`, `userId`, `createdAt`, `updatedAt` are optional
- Fields may be undefined based on viewer permissions
- Contains 30+ fields across three tiers

**ProfileUpdateDTO:**
- Contains only editable fields
- All fields are optional for partial updates
- Maps to backend `ProfileUpdateDTO`

**FieldMetadata:**
- Configuration for rendering individual fields
- Properties:
  - `key`: Field name from ProfileDTO
  - `label`: Display label
  - `fieldType`: Classification (SYSTEM_MANAGED, NON_SENSITIVE, SENSITIVE)
  - `editable`: Whether field can be edited
  - `type`: Input type for rendering
  - `placeholder`: Placeholder text (optional)
  - `selectOptions`: Options for select inputs (optional)

## API Integration

### API Service

**Location:** `frontend/src/features/profile/api/profileApi.ts`

#### `getProfile(userId: string)`

Fetches profile by user ID.

**Request:**
```typescript
GET /api/profiles/{userId}
Headers: Authorization: Bearer {token}
```

**Response:**
```typescript
ProfileDTO with fields filtered by permissions
```

**Errors:**
- 404: Profile not found
- 403: Forbidden (authentication required)

#### `updateProfile(userId: string, updates: ProfileUpdateDTO)`

Updates profile fields.

**Request:**
```typescript
PATCH /api/profiles/{userId}
Headers: Authorization: Bearer {token}
Content-Type: application/json
Body: ProfileUpdateDTO with changed fields
```

**Response:**
```typescript
Updated ProfileDTO with fields filtered by permissions
```

**Errors:**
- 404: Profile not found
- 403: Forbidden (insufficient permissions)
- 400: Validation error

## Permission Model

Field visibility and editability are controlled by the backend based on the viewer's relationship to the profile owner:

### SELF (viewing own profile)
- **View:** All fields
- **Edit:** NON_SENSITIVE and SENSITIVE fields

### MANAGER (viewing direct report)
- **View:** SYSTEM_MANAGED, NON_SENSITIVE, and SENSITIVE fields
- **Edit:** NON_SENSITIVE fields only

### COWORKER
- **View:** SYSTEM_MANAGED and NON_SENSITIVE fields only
- **Edit:** None

The frontend receives pre-filtered data from the backend. Undefined fields indicate the viewer lacks permission to see them.

## Field Categories

### SYSTEM_MANAGED (Not Editable)
- Legal First Name
- Legal Last Name
- Department
- Job Code
- Job Family
- Job Level
- Employment Status
- Hire Date
- Termination Date
- FTE

### NON_SENSITIVE (Editable by SELF and MANAGER)
- Preferred Name
- Job Title
- Office Location
- Work Phone
- Work Location Type
- Bio
- Skills
- Profile Photo URL

### SENSITIVE (Editable by SELF Only)
- Personal Email
- Personal Phone
- Home Address
- Emergency Contact Name
- Emergency Contact Phone
- Emergency Contact Relationship
- Date of Birth
- Visa/Work Permit
- Absence Balance Days (not editable)
- Salary (not editable)
- Performance Rating (not editable)

## Routing

**Profile Page Route:**
```typescript
/profile/:userId
```

**Integration:**
- Added to `App.tsx` as protected route
- HomePage includes "View My Profile" button
- Button navigates to `/profile/{currentUserId}`

## Styling

**Location:** `frontend/src/features/profile/pages/ProfilePage.css`

**Design Features:**
- Clean, modern card-based layout
- Responsive grid for field display
- Clear visual hierarchy with section dividers
- Form inputs with focus states
- Primary/secondary button styles
- Error message styling
- Mobile-responsive breakpoints

**Key CSS Classes:**
- `.profile-page`: Main container (max-width 1000px)
- `.profile-header`: Header with title and actions
- `.profile-section`: Card for field groups
- `.profile-field`: Individual field container
- `.profile-field-input`: Input styling with focus states
- `.btn-primary`, `.btn-secondary`: Button variants

## User Flow

### Viewing Own Profile

1. User clicks "View My Profile" on HomePage
2. Navigates to `/profile/{userId}`
3. ProfilePage fetches profile data via `getProfile(userId)`
4. All fields visible (SELF relationship)
5. "Edit Profile" button appears

### Editing Own Profile

1. User clicks "Edit Profile"
2. Component enters edit mode (`isEditMode = true`)
3. Editable fields render as inputs
4. User modifies field values
5. Changes stored in `editedProfile` state
6. User clicks "Save Changes"
7. Sends `updateProfile(userId, editedProfile)` with only changed fields
8. On success: Updates profile state, exits edit mode
9. On error: Displays error message, remains in edit mode

### Viewing Coworker Profile

1. User navigates to `/profile/{coworkerId}`
2. ProfilePage fetches profile data
3. Only SYSTEM_MANAGED and NON_SENSITIVE fields visible
4. No "Edit Profile" button (not self)
5. Read-only view

### Viewing Direct Report (as Manager)

1. Manager navigates to `/profile/{directReportId}`
2. ProfilePage fetches profile data
3. SYSTEM_MANAGED, NON_SENSITIVE, and SENSITIVE fields visible
4. "Edit Profile" button appears (managers can edit direct reports)
5. In edit mode: Only NON_SENSITIVE fields are editable

## Error Handling

### Loading Errors
- Network failures during fetch
- 404 if profile doesn't exist
- 403 if user not authenticated
- Displays error message with "Back to Home" button

### Update Errors
- Network failures during save
- 403 if insufficient permissions
- 400 if validation fails
- Displays error message inline
- Keeps user in edit mode with changes preserved

### Validation
- Backend validates all updates
- Email format validation
- Required field validation
- Frontend shows backend error messages

## Testing Checklist

### Manual Testing

- [ ] View own profile shows all fields
- [ ] Edit mode enables editable fields only
- [ ] Save changes persists updates
- [ ] Cancel discards unsaved changes
- [ ] View coworker profile hides sensitive fields
- [ ] Manager can view direct report sensitive fields
- [ ] Manager can edit direct report non-sensitive fields
- [ ] Manager cannot edit direct report sensitive fields
- [ ] Invalid email shows validation error
- [ ] 404 error for non-existent profile
- [ ] 403 error without authentication
- [ ] Loading states display correctly
- [ ] Responsive layout on mobile

## Future Enhancements

Potential improvements not included in Story 2.3:

- Client-side field validation before submit
- Optimistic UI updates
- Profile photo upload
- Field-level edit mode (edit one field at a time)
- Audit log of changes
- Bulk field updates
- Profile completeness indicator
- Search/filter for viewing other profiles

## Dependencies

### External Libraries
- `react`: ^18.3.1
- `react-router-dom`: ^7.1.1
- `axios`: ^1.7.9

### Internal Dependencies
- `features/auth/contexts/AuthContext`: User authentication state
- `lib/http-client`: Configured Axios instance with JWT interceptor

## Related Stories

- **Story 1.2.2**: Frontend Authentication (Login, AuthContext)
- **Story 2.1**: Employee Profile Entity & Repository
- **Story 2.2**: Profile API Endpoints (Backend)
- **Story 1.2.3**: Permission Service (Relationship detection)

## Implementation Notes

1. **User ID from JWT**: Backend stores UUID in UserDetails username field for easy extraction
2. **Partial Updates**: Only changed fields sent to backend, not entire profile
3. **Real-time Preview**: Edits merged into display immediately for better UX
4. **Permission Transparency**: Frontend doesn't enforce permissions, trusts backend filtering
5. **Error Messages**: Uses backend error messages for consistency
6. **Date Formatting**: Uses browser locale for date display
7. **Enum Mapping**: TypeScript enums match backend exactly for type safety

## Acceptance Criteria

✅ React components for profile viewing and editing
✅ Permission-based field visibility
✅ Inline edit mode with save/cancel
✅ Integration with Profile API endpoints
✅ Responsive design with clean UI
✅ Error handling for all API operations
✅ Loading states during async operations
✅ Navigation from HomePage to profile
✅ TypeScript types matching backend DTOs
✅ Proper routing with protected routes

## Commit Information

**Story:** 2.3 - Profile Frontend UI
**Branch:** master
**Commit Message:**
```
feat: implement profile frontend UI with view/edit modes (Story 2.3)

- Create ProfileDTO and ProfileUpdateDTO TypeScript types
- Build ProfileField component for field display and editing
- Build ProfileSection component for field grouping
- Implement ProfilePage with view/edit modes and API integration
- Add profile route to App.tsx (/profile/:userId)
- Update HomePage with "View My Profile" button
- Style profile UI with responsive CSS
- Handle loading states, errors, and permissions
- Document frontend profile implementation

Tests: Manual testing required for UI interactions
Related: Stories 1.2.2, 1.2.3, 2.1, 2.2
```
