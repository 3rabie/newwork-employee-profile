/**
 * Employee Profile Types
 *
 * TypeScript types matching backend DTOs for employee profile management.
 * These types align with the backend ProfileDTO and ProfileUpdateDTO.
 */

/**
 * Employment status enum matching backend EmploymentStatus
 */
export enum EmploymentStatus {
  ACTIVE = 'ACTIVE',
  ON_LEAVE = 'ON_LEAVE',
  TERMINATED = 'TERMINATED'
}

/**
 * Work location type enum matching backend WorkLocationType
 */
export enum WorkLocationType {
  OFFICE = 'OFFICE',
  REMOTE = 'REMOTE',
  HYBRID = 'HYBRID'
}

/**
 * Relationship enum matching backend Relationship
 */
export enum Relationship {
  SELF = 'SELF',
  MANAGER = 'MANAGER',
  COWORKER = 'COWORKER'
}

/**
 * Field type enum matching backend FieldType
 */
export enum FieldType {
  SYSTEM_MANAGED = 'SYSTEM_MANAGED',
  NON_SENSITIVE = 'NON_SENSITIVE',
  SENSITIVE = 'SENSITIVE'
}

/**
 * Complete profile DTO matching backend ProfileDTO.
 * Fields may be null based on viewer's permissions.
 */
export interface ProfileDTO {
  // Metadata
  id: string;
  userId: string;
  createdAt: string;
  updatedAt: string;

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
