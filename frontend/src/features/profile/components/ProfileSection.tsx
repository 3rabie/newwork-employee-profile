/**
 * ProfileSection Component
 *
 * Groups related profile fields into collapsible sections.
 * Organizes fields by type: SYSTEM_MANAGED, NON_SENSITIVE, SENSITIVE.
 */

import React from 'react';
import ProfileField from './ProfileField';
import type { ProfileDTO, FieldMetadata } from '../types';
import { FieldType } from '../types';

interface ProfileSectionProps {
  title: string;
  fields: FieldMetadata[];
  profile: ProfileDTO;
  isEditMode: boolean;
  onChange?: (key: string, value: string) => void;
  fieldErrors?: Record<string, string>;
}

const ProfileSection: React.FC<ProfileSectionProps> = ({
  title,
  fields,
  profile,
  isEditMode,
  onChange,
  fieldErrors = {}
}) => {
  const visibleFieldTypes = new Set<string>(
    profile.metadata?.visibleFields ?? Object.values(FieldType)
  );
  const editableFieldTypes = new Set<string>(
    profile.metadata?.editableFields ?? []
  );

  const adjustedFields = fields
    .filter((field) => visibleFieldTypes.has(field.fieldType))
    .map((field) => ({
      ...field,
      editable: field.editable && editableFieldTypes.has(field.fieldType),
    }))
    .filter((field) => {
      const value = profile[field.key];
      return value !== undefined || (isEditMode && field.editable);
    });

  if (adjustedFields.length === 0) {
    return null;
  }

  return (
    <div className="profile-section">
      <h2 className="profile-section-title">{title}</h2>
      <div className="profile-section-fields">
        {adjustedFields.map((field) => (
          <ProfileField
            key={field.key}
            metadata={field}
            value={profile[field.key]}
            isEditMode={isEditMode}
            onChange={onChange}
            error={fieldErrors[field.key as string]}
          />
        ))}
      </div>
    </div>
  );
};

export default ProfileSection;
