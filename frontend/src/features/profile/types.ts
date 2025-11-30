/**
 * Employee Profile Types
 *
 * TypeScript types matching backend DTOs for employee profile management.
 * These types align with the backend ProfileDTO and ProfileUpdateDTO.
 */

/**
 * Employment status constants matching backend EmploymentStatus.
 * Using const objects keeps the values erasable for TypeScript build settings.
 */
export const EmploymentStatus = {
  ACTIVE: 'ACTIVE',
  ON_LEAVE: 'ON_LEAVE',
  TERMINATED: 'TERMINATED',
} as const;
export type EmploymentStatus =
  (typeof EmploymentStatus)[keyof typeof EmploymentStatus];

/**
 * Work location type constants matching backend WorkLocationType.
 */
export const WorkLocationType = {
  OFFICE: 'OFFICE',
  REMOTE: 'REMOTE',
  HYBRID: 'HYBRID',
} as const;
export type WorkLocationType =
  (typeof WorkLocationType)[keyof typeof WorkLocationType];

/**
 * Relationship constants matching backend Relationship.
 */
export const Relationship = {
  SELF: 'SELF',
  MANAGER: 'MANAGER',
  COWORKER: 'COWORKER',
} as const;
export type Relationship =
  (typeof Relationship)[keyof typeof Relationship];

/**
 * Field type constants matching backend FieldType.
 */
export const FieldType = {
  SYSTEM_MANAGED: 'SYSTEM_MANAGED',
  NON_SENSITIVE: 'NON_SENSITIVE',
  SENSITIVE: 'SENSITIVE',
} as const;
export type FieldType = (typeof FieldType)[keyof typeof FieldType];

/**
 * Complete profile DTO matching backend ProfileDTO.
 * Fields may be null based on viewer's permissions.
 */
export interface ProfileMetadata {
  relationship: string;
  visibleFields: string[];
  editableFields: string[];
}

export interface ProfileDTO {
  id: string;
  userId: string;
  email?: string;
  employeeId?: string;
  createdAt?: string;
  updatedAt?: string;

  // SYSTEM_MANAGED fields - visible to all
  legalFirstName?: string;
  legalLastName?: string;
  department?: string;
  jobCode?: string;
  jobFamily?: string;
  jobLevel?: string;
  employmentStatus?: EmploymentStatus;
  hireDate?: string;
  terminationDate?: string | null;
  fte?: number;

  // NON_SENSITIVE fields - visible to all
  preferredName?: string;
  jobTitle?: string;
  officeLocation?: string;
  workPhone?: string;
  workLocationType?: WorkLocationType;
  bio?: string;
  skills?: string;
  profilePhotoUrl?: string | null;

  // SENSITIVE fields - visible to SELF and MANAGER only
  personalEmail?: string;
  personalPhone?: string;
  homeAddress?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  dateOfBirth?: string;
  visaWorkPermit?: string;
  absenceBalanceDays?: number;
  salary?: number;
  performanceRating?: string;

  metadata?: ProfileMetadata;
}

/**
 * Profile update DTO matching backend ProfileUpdateDTO.
 * Contains only editable fields with optional values for partial updates.
 */
export interface ProfileUpdateDTO {
  // NON_SENSITIVE fields - editable by SELF and MANAGER
  preferredName?: string;
  jobTitle?: string;
  officeLocation?: string;
  workPhone?: string;
  workLocationType?: WorkLocationType;
  bio?: string;
  skills?: string;
  profilePhotoUrl?: string;

  // SENSITIVE fields - editable by SELF only
  personalEmail?: string;
  personalPhone?: string;
  homeAddress?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  dateOfBirth?: string;
  visaWorkPermit?: string;
}

/**
 * Field metadata for permission checking and UI rendering
 */
export interface FieldMetadata {
  key: keyof ProfileDTO;
  label: string;
  fieldType: FieldType;
  editable: boolean;
  type: 'text' | 'email' | 'tel' | 'textarea' | 'date' | 'number' | 'select';
  placeholder?: string;
  selectOptions?: Array<{ value: string; label: string }>;
}
