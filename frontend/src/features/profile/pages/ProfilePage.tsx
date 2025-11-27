/**
 * ProfilePage Component
 *
 * Main page for viewing and editing employee profiles.
 * Handles permission-based field visibility and inline editing.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/contexts/AuthContext';
import ProfileSection from '../components/ProfileSection';
import { getProfile, updateProfile } from '../api/profileApi';
import { ProfileDTO, ProfileUpdateDTO, FieldMetadata, FieldType } from '../types';
import './ProfilePage.css';

const ProfilePage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [profile, setProfile] = useState<ProfileDTO | null>(null);
  const [editedProfile, setEditedProfile] = useState<ProfileUpdateDTO>({});
  const [isEditMode, setIsEditMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isSelf = user?.id === userId;

  useEffect(() => {
    if (userId) {
      loadProfile(userId);
    }
  }, [userId]);

  const loadProfile = async (id: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await getProfile(id);
      setProfile(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = () => {
    setIsEditMode(true);
    setEditedProfile({});
  };

  const handleCancel = () => {
    setIsEditMode(false);
    setEditedProfile({});
  };

  const handleSave = async () => {
    if (!userId || Object.keys(editedProfile).length === 0) {
      setIsEditMode(false);
      return;
    }

    try {
      setSaving(true);
      setError(null);
      const updated = await updateProfile(userId, editedProfile);
      setProfile(updated);
      setIsEditMode(false);
      setEditedProfile({});
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save changes');
    } finally {
      setSaving(false);
    }
  };

  const handleFieldChange = (key: string, value: string) => {
    setEditedProfile((prev) => ({
      ...prev,
      [key]: value
    }));
  };

  // Field metadata for all profile fields
  const systemManagedFields: FieldMetadata[] = [
    { key: 'legalFirstName', label: 'Legal First Name', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'text' },
    { key: 'legalLastName', label: 'Legal Last Name', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'text' },
    { key: 'department', label: 'Department', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'text' },
    { key: 'jobCode', label: 'Job Code', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'text' },
    { key: 'jobFamily', label: 'Job Family', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'text' },
    { key: 'jobLevel', label: 'Job Level', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'text' },
    { key: 'employmentStatus', label: 'Employment Status', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'select' },
    { key: 'hireDate', label: 'Hire Date', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'date' },
    { key: 'terminationDate', label: 'Termination Date', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'date' },
    { key: 'fte', label: 'FTE', fieldType: FieldType.SYSTEM_MANAGED, editable: false, type: 'number' }
  ];

  const nonSensitiveFields: FieldMetadata[] = [
    { key: 'preferredName', label: 'Preferred Name', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'text', placeholder: 'Enter preferred name' },
    { key: 'jobTitle', label: 'Job Title', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'text', placeholder: 'Enter job title' },
    { key: 'officeLocation', label: 'Office Location', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'text', placeholder: 'Enter office location' },
    { key: 'workPhone', label: 'Work Phone', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'tel', placeholder: '+1-555-0100' },
    { key: 'workLocationType', label: 'Work Location Type', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'select' },
    { key: 'bio', label: 'Bio', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'textarea', placeholder: 'Tell us about yourself' },
    { key: 'skills', label: 'Skills', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'textarea', placeholder: 'List your skills' },
    { key: 'profilePhotoUrl', label: 'Profile Photo URL', fieldType: FieldType.NON_SENSITIVE, editable: true, type: 'text', placeholder: 'https://...' }
  ];

  const sensitiveFields: FieldMetadata[] = [
    { key: 'personalEmail', label: 'Personal Email', fieldType: FieldType.SENSITIVE, editable: true, type: 'email', placeholder: 'personal@email.com' },
    { key: 'personalPhone', label: 'Personal Phone', fieldType: FieldType.SENSITIVE, editable: true, type: 'tel', placeholder: '+1-555-9999' },
    { key: 'homeAddress', label: 'Home Address', fieldType: FieldType.SENSITIVE, editable: true, type: 'textarea', placeholder: 'Enter home address' },
    { key: 'emergencyContactName', label: 'Emergency Contact Name', fieldType: FieldType.SENSITIVE, editable: true, type: 'text', placeholder: 'Contact name' },
    { key: 'emergencyContactPhone', label: 'Emergency Contact Phone', fieldType: FieldType.SENSITIVE, editable: true, type: 'tel', placeholder: '+1-555-0000' },
    { key: 'emergencyContactRelationship', label: 'Emergency Contact Relationship', fieldType: FieldType.SENSITIVE, editable: true, type: 'text', placeholder: 'Relationship' },
    { key: 'dateOfBirth', label: 'Date of Birth', fieldType: FieldType.SENSITIVE, editable: true, type: 'date' },
    { key: 'visaWorkPermit', label: 'Visa/Work Permit', fieldType: FieldType.SENSITIVE, editable: true, type: 'text', placeholder: 'Visa details' },
    { key: 'absenceBalanceDays', label: 'Absence Balance (Days)', fieldType: FieldType.SENSITIVE, editable: false, type: 'number' },
    { key: 'salary', label: 'Salary', fieldType: FieldType.SENSITIVE, editable: false, type: 'number' },
    { key: 'performanceRating', label: 'Performance Rating', fieldType: FieldType.SENSITIVE, editable: false, type: 'text' }
  ];

  if (loading) {
    return (
      <div className="profile-page">
        <div className="profile-loading">Loading profile...</div>
      </div>
    );
  }

  if (error && !profile) {
    return (
      <div className="profile-page">
        <div className="profile-error">
          <h2>Error</h2>
          <p>{error}</p>
          <button onClick={() => navigate('/')}>Back to Home</button>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="profile-page">
        <div className="profile-error">Profile not found</div>
      </div>
    );
  }

  // Merge edited values into profile for display
  const displayProfile = {
    ...profile,
    ...Object.fromEntries(
      Object.entries(editedProfile).filter(([_, value]) => value !== undefined && value !== '')
    )
  };

  return (
    <div className="profile-page">
      <div className="profile-header">
        <div className="profile-header-info">
          <h1 className="profile-title">
            {profile.preferredName || profile.legalFirstName} {profile.legalLastName}
          </h1>
          <p className="profile-subtitle">{profile.jobTitle || 'Employee'}</p>
        </div>
        <div className="profile-header-actions">
          {!isEditMode ? (
            <>
              <button className="btn-secondary" onClick={() => navigate('/')}>
                Back
              </button>
              {isSelf && (
                <button className="btn-primary" onClick={handleEdit}>
                  Edit Profile
                </button>
              )}
            </>
          ) : (
            <>
              <button className="btn-secondary" onClick={handleCancel} disabled={saving}>
                Cancel
              </button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </>
          )}
        </div>
      </div>

      {error && (
        <div className="profile-error-message">
          {error}
        </div>
      )}

      <div className="profile-content">
        <ProfileSection
          title="Basic Information"
          fields={systemManagedFields}
          profile={displayProfile}
          isEditMode={isEditMode}
          onChange={handleFieldChange}
        />

        <ProfileSection
          title="Work Details"
          fields={nonSensitiveFields}
          profile={displayProfile}
          isEditMode={isEditMode}
          onChange={handleFieldChange}
        />

        <ProfileSection
          title="Personal & Confidential"
          fields={sensitiveFields}
          profile={displayProfile}
          isEditMode={isEditMode}
          onChange={handleFieldChange}
        />
      </div>
    </div>
  );
};

export default ProfilePage;
