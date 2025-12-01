import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/contexts/AuthContext';
import {
  getMyAbsenceRequests,
  getPendingAbsenceRequests,
  submitAbsenceRequest,
  updateAbsenceStatus
} from '../api/absenceApi';
import type {
  AbsenceRequest,
  AbsenceType,
  CreateAbsenceRequestInput
} from '../types';
import { graphqlRequest } from '../../../lib/graphql-client';
import { GET_COWORKER_DIRECTORY_QUERY } from '../../../lib/graphql-queries';
import './AbsencePage.css';

export const AbsencePage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isManager = user?.role === 'MANAGER';

  const [myRequests, setMyRequests] = useState<AbsenceRequest[]>([]);
  const [pendingRequests, setPendingRequests] = useState<AbsenceRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingError, setPendingError] = useState<string | null>(null);
  const [form, setForm] = useState<CreateAbsenceRequestInput>({
    startDate: '',
    endDate: '',
    type: 'VACATION',
    note: ''
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [personLookup, setPersonLookup] = useState<Record<string, { name: string; employeeId?: string }>>({});

  useEffect(() => {
    loadMyRequests();
  }, []);

  useEffect(() => {
    if (isManager) {
      loadPending();
      loadDirectory();
    }
  }, [isManager]);

  const loadMyRequests = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getMyAbsenceRequests();
      setMyRequests(data);
    } catch (err) {
      setError('Failed to load your absence requests');
    } finally {
      setLoading(false);
    }
  };

  const loadPending = async () => {
    try {
      setPendingLoading(true);
      setPendingError(null);
      const data = await getPendingAbsenceRequests();
      setPendingRequests(data);
    } catch (err) {
      setPendingError('Failed to load pending requests');
    } finally {
      setPendingLoading(false);
    }
  };

  const loadDirectory = async () => {
    try {
      const data = await graphqlRequest<{ coworkerDirectory: Array<{ userId: string; preferredName?: string; legalFirstName: string; legalLastName: string; employeeId: string }> }>(
        GET_COWORKER_DIRECTORY_QUERY
      );
      const map = data.coworkerDirectory.reduce<Record<string, { name: string; employeeId: string }>>((acc, person) => {
        const name = person.preferredName
          ? `${person.preferredName} ${person.legalLastName}`
          : `${person.legalFirstName} ${person.legalLastName}`;
        acc[person.userId] = { name, employeeId: person.employeeId };
        return acc;
      }, {});
      setPersonLookup(map);
    } catch (err) {
      // best-effort; keep empty map
    }
  };

  const validateForm = (input: CreateAbsenceRequestInput) => {
    const errors: Record<string, string> = {};
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (!input.startDate) errors.startDate = 'Start date is required';
    if (!input.endDate) errors.endDate = 'End date is required';
    if (input.startDate && input.endDate) {
      const start = new Date(input.startDate);
      const end = new Date(input.endDate);
      const isSick = input.type === 'SICK';
      if (!isSick && start < today) {
        errors.startDate = 'Start date must be today or later';
      }
      if (!isSick && end < today) {
        errors.endDate = 'End date must be today or later';
      }
      if (end < start) {
        errors.endDate = 'End date cannot be before start date';
      }
    }
    if (input.note && input.note.length > 200) {
      errors.note = 'Note must be 200 characters or less';
    }
    return errors;
  };

  const handleSubmit = async () => {
    const validationErrors = validateForm(form);
    if (Object.keys(validationErrors).length) {
      setFormErrors(validationErrors);
      return;
    }
    try {
      setSubmitting(true);
      await submitAbsenceRequest(form);
      setForm({
        startDate: '',
        endDate: '',
        type: form.type as AbsenceType,
        note: ''
      });
      await loadMyRequests();
    } catch (err) {
      setError('Failed to submit request');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDecision = async (id: string, action: 'APPROVE' | 'REJECT') => {
    const note =
      action === 'REJECT'
        ? window.prompt('Add a note for the rejection (optional)') ?? undefined
        : undefined;
    try {
      await updateAbsenceStatus(id, { action, note });
      await Promise.all([loadMyRequests(), loadPending()]);
    } catch {
      setPendingError('Failed to update request');
    }
  };

  const sortedMine = [...myRequests].sort(
    (a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
  );
  const sortedPending = [...pendingRequests].sort(
    (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
  );

  return (
    <div className="absence-page">
      <header className="absence-header">
        <div>
          <p className="eyebrow">Absence</p>
          <h1>Absence Requests</h1>
          <p className="subtitle">
            Submit your time off and, if you are a manager, review pending approvals.
          </p>
        </div>
        <div className="absence-header-actions">
          <button className="btn-secondary btn-back" onClick={() => navigate(-1)}>
            Back
          </button>
          {isManager && (
            <div className="pending-pill">
              Pending approvals: <strong>{pendingRequests.length}</strong>
            </div>
          )}
        </div>
      </header>

      <section className="absence-card">
        <div className="absence-card__header">
          <h2>Submit a Request</h2>
        </div>
        <div className="form-row">
          <div className="form-field">
            <label htmlFor="abs-type">Type</label>
            <select
              id="abs-type"
              value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value as AbsenceType })}
            >
              <option value="VACATION">Vacation</option>
              <option value="SICK">Sick</option>
              <option value="PERSONAL">Personal</option>
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="abs-start">Start</label>
            <input
              id="abs-start"
              type="date"
              value={form.startDate}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })}
            />
            {formErrors.startDate && <span className="field-error">{formErrors.startDate}</span>}
          </div>
          <div className="form-field">
            <label htmlFor="abs-end">End</label>
            <input
              id="abs-end"
              type="date"
              value={form.endDate}
              onChange={(e) => setForm({ ...form, endDate: e.target.value })}
            />
            {formErrors.endDate && <span className="field-error">{formErrors.endDate}</span>}
          </div>
        </div>
        <div className="form-field">
          <label htmlFor="abs-note">Note (optional, 200 chars)</label>
          <textarea
            id="abs-note"
            maxLength={200}
            value={form.note || ''}
            onChange={(e) => setForm({ ...form, note: e.target.value })}
          />
          {formErrors.note && <span className="field-error">{formErrors.note}</span>}
        </div>
        <div className="form-actions">
          <button className="btn-primary" onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Request'}
          </button>
          {error && <span className="field-error">{error}</span>}
        </div>
      </section>

      <section className="absence-card">
        <div className="absence-card__header">
          <h2>My Requests</h2>
        </div>
        {loading ? (
          <p>Loading...</p>
        ) : sortedMine.length === 0 ? (
          <p className="muted">No requests yet.</p>
        ) : (
          <ul className="absence-list__items">
            {sortedMine.map((item) => (
              <li key={item.id} className="absence-list__item">
                <div className="absence-list__item-header">
                  <span className={`status-badge status-${item.status.toLowerCase()}`}>
                    {item.status}
                  </span>
                  <span className="absence-dates">
                    {item.startDate} → {item.endDate}
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
      </section>

      {isManager && (
        <section className="absence-card">
          <div className="absence-card__header">
            <div>
              <p className="eyebrow">Team View</p>
              <h2>Pending Approvals</h2>
            </div>
          </div>
          {pendingLoading ? (
            <p>Loading pending requests...</p>
          ) : pendingError ? (
            <p className="field-error">{pendingError}</p>
          ) : sortedPending.length === 0 ? (
            <p className="muted">No pending requests.</p>
          ) : (
            <ul className="absence-list__items">
              {sortedPending.map((item) => (
                <li key={item.id} className="absence-list__item">
                  <div className="absence-list__item-header">
                    <span className="absence-dates">
                      {item.startDate} → {item.endDate}
                    </span>
                    <span className="pill">{item.type}</span>
                  </div>
                  <div className="absence-meta">
                    <span>
                      {personLookup[item.userId]?.name || item.userId}
                      {personLookup[item.userId]?.employeeId
                        ? ` · ${personLookup[item.userId]?.employeeId}`
                        : ''}
                    </span>
                    {item.note && <span className="muted">Note: {item.note}</span>}
                  </div>
                  <div className="form-actions">
                    <button
                      className="btn-secondary"
                      onClick={() => handleDecision(item.id, 'REJECT')}
                    >
                      Reject
                    </button>
                    <button
                      className="btn-primary"
                      onClick={() => handleDecision(item.id, 'APPROVE')}
                    >
                      Approve
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
};
