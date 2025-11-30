/**
 * ProfilePage Component
 *
 * Main page for viewing and editing employee profiles.
 * Handles permission-based field visibility and inline editing.
 */

import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/contexts/AuthContext';
import ProfileSection from '../components/ProfileSection';
import { getProfile, updateProfile } from '../api/profileApi';
import type { ProfileDTO, ProfileUpdateDTO, FieldMetadata } from '../types';
import { FieldType } from '../types';
import { FeedbackList } from '../../feedback/components/FeedbackList';
import { FeedbackModal } from '../../feedback/components/FeedbackModal';
import { getFeedbackForUser } from '../../feedback/api/feedbackApi';
import type { FeedbackListItem } from '../../feedback/types';
import { GraphQLRequestError } from '../../../lib/graphql-client';
import './ProfilePage.css';

type ApiErrorResponse = {
  message?: string;
};

const getErrorMessage = (error: unknown, fallbackMessage: string): string => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.data?.message) {
      return error.response.data.message;
    }
    if (error.response?.status === 404) {
      return 'Profile not found';
    }
  }
  return fallbackMessage;
};

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
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [feedbackItems, setFeedbackItems] = useState<FeedbackListItem[]>([]);
  const [feedbackLoading, setFeedbackLoading] = useState(true);
  const [feedbackError, setFeedbackError] = useState<string | null>(null);
  const [feedbackForbidden, setFeedbackForbidden] = useState(false);
  const [isFeedbackModalOpen, setFeedbackModalOpen] = useState(false);

  const isSelf = user?.userId === userId;
  const canGiveFeedback = Boolean(user?.userId && userId && user?.userId !== userId);
  const canEditProfile = profile?.metadata
    ? profile.metadata.editableFields.length > 0
    : isSelf;

  useEffect(() => {
    if (userId) {
      loadProfile(userId);
      loadFeedback(userId);
    }
  }, [userId]);

  const loadProfile = async (id: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await getProfile(id);
      setProfile(data);
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Failed to load profile'));
    } finally {
      setLoading(false);
    }
  };

  const loadFeedback = async (id: string) => {
    try {
      setFeedbackLoading(true);
      setFeedbackError(null);
      setFeedbackForbidden(false);
      const data = await getFeedbackForUser(id);
      setFeedbackItems(data);
      setFeedbackForbidden(false);
    } catch (err: unknown) {
      if (err instanceof GraphQLRequestError) {
        const forbiddenError = err.errors?.some(
          (error) => error.extensions?.errorType === 'FORBIDDEN'
        );
        if (forbiddenError) {
            setFeedbackForbidden(true);
            setFeedbackError(null);
          setFeedbackItems([]);
          setFeedbackLoading(false);
          return;
        }
      }
      const message = getErrorMessage(err, 'Failed to load feedback');
        setFeedbackError(message);
    } finally {
      setFeedbackLoading(false);
    }
  };

  const handleEdit = () => {
    if (!canEditProfile) {
      return;
    }
    setIsEditMode(true);
    setEditedProfile({});
    setFieldErrors({});
  };

  const handleReloadFeedback = () => {
    if (userId) {
      loadFeedback(userId);
    }
  };

  const handleCancel = () => {
    setIsEditMode(false);
    setEditedProfile({});
    setFieldErrors({});
  };

  const validateProfileUpdates = (updates: ProfileUpdateDTO) => {
    const errors: Record<string, string> = {};
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const urlRegex = /^https?:\/\/.+/i;

    if (updates.preferredName && updates.preferredName.trim().length < 2) {
      errors.preferredName = 'Preferred name must be at least 2 characters.';
    }

    if (updates.personalEmail && !emailRegex.test(updates.personalEmail)) {
      errors.personalEmail = 'Enter a valid email address.';
    }

    if (updates.profilePhotoUrl && !urlRegex.test(updates.profilePhotoUrl)) {
      errors.profilePhotoUrl = 'Enter a valid URL starting with http or https.';
    }

    const phoneFields: Array<keyof ProfileUpdateDTO> = [
      'workPhone',
      'personalPhone',
      'emergencyContactPhone'
    ];

    phoneFields.forEach((field) => {
      const value = updates[field];
      if (value && value.replace(/\D/g, '').length < 7) {
        errors[field as string] = 'Enter at least 7 digits.';
      }
    });

    if (updates.dateOfBirth) {
      const dateValue = new Date(updates.dateOfBirth);
      if (Number.isNaN(dateValue.getTime()) || dateValue > new Date()) {
        errors.dateOfBirth = 'Date of birth must be in the past.';
      }
    }

    if (updates.bio && updates.bio.length > 1000) {
      errors.bio = 'Bio must be 1000 characters or fewer.';
    }

    if (updates.skills && updates.skills.length > 1000) {
      errors.skills = 'Skills must be 1000 characters or fewer.';
    }

    return errors;
  };

  const handleSave = async () => {
    if (!userId || Object.keys(editedProfile).length === 0) {
      setIsEditMode(false);
      setFieldErrors({});
      return;
    }

    const validationErrors = validateProfileUpdates(editedProfile);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setError('Please fix the highlighted fields.');
      return;
    }

    const pendingUpdates = editedProfile;
    const previousProfile = profile;

    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      if (previousProfile) {
        setProfile({ ...previousProfile, ...pendingUpdates });
      }
      setIsEditMode(false);
      setEditedProfile({});
      const updated = await updateProfile(userId, pendingUpdates);
      setProfile(updated);
    } catch (err: unknown) {
      if (previousProfile) {
        setProfile(previousProfile);
      }
      setEditedProfile(pendingUpdates);
      setIsEditMode(true);
      setError(getErrorMessage(err, 'Failed to save changes'));
    } finally {
      setSaving(false);
    }
  };

  const handleFieldChange = (key: string, value: string) => {
    setEditedProfile((prev) => ({
      ...prev,
      [key]: value
    }));
    setFieldErrors((prev) => {
      if (!prev[key]) {
        return prev;
      }
      const { [key]: _, ...rest } = prev;
      return rest;
    });
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
      Object.entries(editedProfile).filter(([_, value]) => value !== undefined)
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
              <button
                className="btn-secondary"
                onClick={() => navigate(isSelf ? '/' : '/people')}
              >
                Back
              </button>
              {canEditProfile && (
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
          fieldErrors={fieldErrors}
        />

        <ProfileSection
          title="Work Details"
          fields={nonSensitiveFields}
          profile={displayProfile}
          isEditMode={isEditMode}
          onChange={handleFieldChange}
          fieldErrors={fieldErrors}
        />

        <ProfileSection
          title="Personal & Confidential"
          fields={sensitiveFields}
          profile={displayProfile}
          isEditMode={isEditMode}
          onChange={handleFieldChange}
          fieldErrors={fieldErrors}
        />

        <section className="profile-panel">
          <div className="profile-panel__header">
            <div>
              <h2>Feedback</h2>
              <p className="profile-panel__helper">
                Feedback is private between the author, recipient, and their managers.
              </p>
            </div>
            {canGiveFeedback && (
              <button className="btn-primary" onClick={() => setFeedbackModalOpen(true)}>
                Give Feedback
              </button>
            )}
          </div>
          {feedbackForbidden ? null : feedbackError ? (
            <div className="profile-error-message">
              <p>{feedbackError}</p>
              <button className="btn-secondary" onClick={handleReloadFeedback}>
                Retry loading feedback
              </button>
            </div>
          ) : (
            <FeedbackList
              items={feedbackItems}
              isLoading={feedbackLoading}
              emptyState="No feedback recieved yet."
            />
          )}
        </section>
      </div>
      {canGiveFeedback && (
        <FeedbackModal
          recipientId={profile.userId}
          recipientDisplayName={`${profile.preferredName || profile.legalFirstName} ${profile.legalLastName}`}
          isOpen={isFeedbackModalOpen}
          onClose={() => setFeedbackModalOpen(false)}
          onCreated={(item) => setFeedbackItems((prev) => [item, ...prev])}
        />
      )}
    </div>
  );
};

export default ProfilePage;
