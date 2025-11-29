/**
 * ProfileField Component
 *
 * Reusable component for displaying and editing individual profile fields.
 * Supports view and edit modes with different input types.
 */

import React from 'react';
import type { FieldMetadata } from '../types';
import { WorkLocationType, EmploymentStatus } from '../types';

interface ProfileFieldProps {
  metadata: FieldMetadata;
  value: string | number | undefined | null;
  isEditMode: boolean;
  onChange?: (key: string, value: string) => void;
  error?: string;
}

const ProfileField: React.FC<ProfileFieldProps> = ({
  metadata,
  value,
  isEditMode,
  onChange,
  error
}) => {
  const displayValue = value ?? 'N/A';

  const handleChange = (newValue: string) => {
    if (onChange) {
      onChange(metadata.key, newValue);
    }
  };

  // View mode - display only
  if (!isEditMode || !metadata.editable) {
    return (
      <div className="profile-field">
        <label className="profile-field-label">{metadata.label}</label>
        <div className="profile-field-value">
          {metadata.type === 'date' && value
            ? new Date(value as string).toLocaleDateString()
            : displayValue}
        </div>
      </div>
    );
  }

  // Edit mode - render appropriate input
  const renderInput = () => {
    switch (metadata.type) {
      case 'textarea':
        return (
          <textarea
            className="profile-field-input profile-field-textarea"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            placeholder={metadata.placeholder}
            rows={4}
          />
        );

      case 'select':
        if (metadata.key === 'workLocationType') {
          return (
            <select
              className="profile-field-input profile-field-select"
              value={value || ''}
              onChange={(e) => handleChange(e.target.value)}
            >
              <option value="">Select...</option>
              <option value={WorkLocationType.OFFICE}>Office</option>
              <option value={WorkLocationType.REMOTE}>Remote</option>
              <option value={WorkLocationType.HYBRID}>Hybrid</option>
            </select>
          );
        }
        if (metadata.key === 'employmentStatus') {
          return (
            <select
              className="profile-field-input profile-field-select"
              value={value || ''}
              onChange={(e) => handleChange(e.target.value)}
            >
              <option value="">Select...</option>
              <option value={EmploymentStatus.ACTIVE}>Active</option>
              <option value={EmploymentStatus.ON_LEAVE}>On Leave</option>
              <option value={EmploymentStatus.TERMINATED}>Terminated</option>
            </select>
          );
        }
        return null;

      case 'date':
        return (
          <input
            type="date"
            className="profile-field-input"
            value={value ? new Date(value as string).toISOString().split('T')[0] : ''}
            onChange={(e) => handleChange(e.target.value)}
          />
        );

      case 'number':
        return (
          <input
            type="number"
            className="profile-field-input"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            placeholder={metadata.placeholder}
            step={metadata.key === 'salary' || metadata.key === 'fte' ? '0.01' : '1'}
          />
        );

      case 'email':
        return (
          <input
            type="email"
            className="profile-field-input"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            placeholder={metadata.placeholder}
          />
        );

      case 'tel':
        return (
          <input
            type="tel"
            className="profile-field-input"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            placeholder={metadata.placeholder}
          />
        );

      default:
        return (
          <input
            type="text"
            className="profile-field-input"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            placeholder={metadata.placeholder}
          />
        );
    }
  };

  return (
    <div className="profile-field">
      <label className="profile-field-label">{metadata.label}</label>
      {renderInput()}
      {error && <p className="profile-field-error">{error}</p>}
    </div>
  );
};

export default ProfileField;
