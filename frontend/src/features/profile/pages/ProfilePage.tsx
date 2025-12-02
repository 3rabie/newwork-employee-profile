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
import {
  getMyAbsenceRequests,
  submitAbsenceRequest
} from '../../absence/api/absenceApi';
import type {
  AbsenceRequest,
  CreateAbsenceRequestInput,
  AbsenceType
} from '../../absence/types';
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
  const [absenceRequests, setAbsenceRequests] = useState<AbsenceRequest[]>([]);
  const [absenceLoading, setAbsenceLoading] = useState(false);
  const [absenceError, setAbsenceError] = useState<string | null>(null);
  const [absenceSuccess, setAbsenceSuccess] = useState<string | null>(null);
  const [absenceSubmitting, setAbsenceSubmitting] = useState(false);
  const [absenceForm, setAbsenceForm] = useState<CreateAbsenceRequestInput>({
    startDate: '',
    endDate: '',
    type: 'VACATION',
    note: ''
  });
  const [absenceFormErrors, setAbsenceFormErrors] = useState<Record<string, string>>({});

  const isSelf = user?.userId === userId;
  const canGiveFeedback = Boolean(user?.userId && userId && user?.userId !== userId);
  const canEditProfile = profile?.metadata
    ? profile.metadata.editableFields.length > 0
    : isSelf;

  useEffect(() => {
    if (userId) {
      loadProfile(userId);
      loadFeedback(userId);
      if (isSelf) {
        loadAbsences();
      } else {
        loadDirectory();
      }
    }
  }, [userId, isSelf]);

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

  const loadAbsences = async () => {
    try {
      setAbsenceLoading(true);
      setAbsenceError(null);
      const data = await getMyAbsenceRequests();
      setAbsenceRequests(data);
    } catch (err: unknown) {
      setAbsenceError(getErrorMessage(err, 'Failed to load absences'));
    } finally {
      setAbsenceLoading(false);
    }
  };

  const loadDirectory = async () => {
    try {
      // Person lookup map - reserved for future enhancement
      // await graphqlRequest(GET_COWORKER_DIRECTORY_QUERY);
    } catch {
      // Best effort, ignore errors here
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

  const validateAbsenceForm = (form: CreateAbsenceRequestInput) => {
    const errors: Record<string, string> = {};
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (!form.startDate) {
      errors.startDate = 'Start date is required';
    }
    if (!form.endDate) {
      errors.endDate = 'End date is required';
    }
    if (form.startDate && form.endDate) {
      const start = new Date(form.startDate);
      const end = new Date(form.endDate);
      const isSick = form.type === 'SICK';

      if (!isSick && start < today) {
        errors.startDate = 'Start date must be today or later';
      }
      if (!isSick && end < today) {
        errors.endDate = 'End date must be today or later';
      }
      if (end < start) {
        errors.endDate = 'End date cannot be before start date';
      }
      const maxDurationDays = 30;
      const durationMs = end.getTime() - start.getTime();
      if (!errors.endDate && durationMs / (1000 * 60 * 60 * 24) > maxDurationDays) {
        errors.endDate = 'Absence cannot exceed 30 days';
      }
    }
    if (form.note && form.note.length > 200) {
      errors.note = 'Note must be 200 characters or less';
    }
    return errors;
  };

  const handleAbsenceFormChange = (
    field: keyof CreateAbsenceRequestInput,
    value: string
  ) => {
    setAbsenceForm((prev) => ({
      ...prev,
      [field]: value
    }));
    setAbsenceFormErrors((prev) => {
      if (!prev[field]) return prev;
      const { [field]: _, ...rest } = prev;
      return rest;
    });
  };

  const handleAbsenceSubmit = async () => {
    const validationErrors = validateAbsenceForm(absenceForm);
    if (Object.keys(validationErrors).length > 0) {
      setAbsenceFormErrors(validationErrors);
      return;
    }
    try {
      setAbsenceSubmitting(true);
      setAbsenceError(null);
      setAbsenceSuccess(null);
      await submitAbsenceRequest(absenceForm);
      setAbsenceSuccess('Absence request submitted');
      setAbsenceForm({
        startDate: '',
        endDate: '',
        type: absenceForm.type as AbsenceType,
        note: ''
      });
      await loadAbsences();
    } catch (err: unknown) {
      setAbsenceError(getErrorMessage(err, 'Failed to submit absence request'));
    } finally {
      setAbsenceSubmitting(false);
    }
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

  const sortedAbsences = [...absenceRequests].sort(
    (a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
  );

  return (
    <div className="profile-page">
      <div className="profile-header">
        <div className="profile-header-info">
          <h1 className="profile-title">
            {profile.preferredName || profile.legalFirstName} {profile.legalLastName}
          </h1>
          <p className="profile-subtitle">{profile.jobTitle || 'Employee'}</p>
          {isSelf && (
            <div className="inline-badges">
              <span className="badge-neutral">
                Pending absences:{' '}
                {absenceRequests.filter((a) => a.status === 'PENDING').length}
              </span>
              <span className="badge-neutral">
                Approved:{' '}
                {absenceRequests.filter((a) => a.status === 'APPROVED').length}
              </span>
            </div>
          )}
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

        {isSelf && (
          <section className="profile-panel">
            <div className="profile-panel__header">
              <div>
                <h2>Absence Requests</h2>
                <p className="profile-panel__helper">
                  Submit time off and track approvals.
                </p>
              </div>
            </div>

            <div className="absence-grid">
              <div className="absence-form">
                <div className="form-row">
                  <div className="form-field">
                    <label htmlFor="absence-type">Type</label>
                    <select
                      id="absence-type"
                      value={absenceForm.type}
                      onChange={(e) => handleAbsenceFormChange('type', e.target.value)}
                    >
                      <option value="VACATION">Vacation</option>
                      <option value="SICK">Sick</option>
                      <option value="PERSONAL">Personal</option>
                    </select>
                  </div>
                  <div className="form-field">
                    <label htmlFor="absence-start">Start Date</label>
                    <input
                      id="absence-start"
                      type="date"
                      value={absenceForm.startDate}
                      onChange={(e) => handleAbsenceFormChange('startDate', e.target.value)}
                    />
                    {absenceFormErrors.startDate && (
                      <span className="field-error">{absenceFormErrors.startDate}</span>
                    )}
                  </div>
                  <div className="form-field">
                    <label htmlFor="absence-end">End Date</label>
                    <input
                      id="absence-end"
                      type="date"
                      value={absenceForm.endDate}
                      onChange={(e) => handleAbsenceFormChange('endDate', e.target.value)}
                    />
                    {absenceFormErrors.endDate && (
                      <span className="field-error">{absenceFormErrors.endDate}</span>
                    )}
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-field full-width">
                    <label htmlFor="absence-note">Note (optional)</label>
                    <textarea
                      id="absence-note"
                      maxLength={200}
                      value={absenceForm.note || ''}
                      onChange={(e) => handleAbsenceFormChange('note', e.target.value)}
                      placeholder="Add context for your manager (max 200 chars)"
                    />
                    {absenceFormErrors.note && (
                      <span className="field-error">{absenceFormErrors.note}</span>
                    )}
                  </div>
                </div>
                <div className="form-actions">
                  <button
                    className="btn-primary"
                    onClick={handleAbsenceSubmit}
                    disabled={absenceSubmitting}
                  >
                    {absenceSubmitting ? 'Submitting...' : 'Submit Request'}
                  </button>
                  {absenceSuccess && (
                    <span className="success-text">{absenceSuccess}</span>
                  )}
                  {absenceError && <span className="field-error">{absenceError}</span>}
                </div>
              </div>

              <div className="absence-list">
                <div className="absence-list__header">
                  <h3>My Requests</h3>
                  <span className="badge-neutral">{absenceRequests.length}</span>
                </div>
                {absenceLoading ? (
                  <p>Loading absences...</p>
                ) : absenceError ? (
                  <div className="profile-error-message">{absenceError}</div>
                ) : sortedAbsences.length === 0 ? (
                  <p className="muted">No absence requests yet.</p>
                ) : (
                  <ul className="absence-list__items">
                    {sortedAbsences.map((item) => (
                      <li key={item.id} className="absence-list__item">
                        <div className="absence-list__item-header">
                          <span className={`status-badge status-${item.status.toLowerCase()}`}>
                            {item.status}
                          </span>
                          <span className="absence-dates">
                            {item.startDate} &rarr; {item.endDate}
                          </span>
                        </div>
                        <div className="absence-meta">
                          <span className="pill">{item.type}</span>
                          {item.note && <span className="muted">Note: {item.note}</span>}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </section>
        )}

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




