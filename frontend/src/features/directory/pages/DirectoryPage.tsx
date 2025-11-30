import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCoworkerDirectory } from '../api/directoryApi';
import type { Coworker, DirectoryFilters } from '../types';
import { FeedbackModal } from '../../feedback/components/FeedbackModal';
import './DirectoryPage.css';

interface FormState {
  search: string;
  department: string;
}

const initialFilters: FormState = {
  search: '',
  department: '',
};

export function DirectoryPage() {
  const navigate = useNavigate();
  const [coworkers, setCoworkers] = useState<Coworker[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [appliedFilters, setAppliedFilters] = useState<FormState>(() => ({
    ...initialFilters,
  }));
  const [formState, setFormState] = useState<FormState>(() => ({
    ...initialFilters,
  }));
  const [recipient, setRecipient] = useState<Coworker | null>(null);

  useEffect(() => {
    const loadDirectory = async () => {
      setLoading(true);
      setError(null);
      try {
        const payload: DirectoryFilters = {
          search: appliedFilters.search.trim() || undefined,
          department: appliedFilters.department.trim() || undefined,
        };
        const results = await getCoworkerDirectory(payload);
        setCoworkers(results);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load coworkers');
        setCoworkers([]);
      } finally {
        setLoading(false);
      }
    };

    loadDirectory();
  }, [appliedFilters]);

  const departments = useMemo(() => {
    const unique = new Set<string>();
    coworkers.forEach((person) => {
      if (person.department) {
        unique.add(person.department);
      }
    });
    return Array.from(unique).sort();
  }, [coworkers]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAppliedFilters({ ...formState });
  };

  const resetFilters = () => {
    setFormState({ ...initialFilters });
    setAppliedFilters({ ...initialFilters });
  };

  return (
    <div className="directory-page">
      <header className="directory-header">
        <div className="directory-header__content">
          <div>
            <h1>People Directory</h1>
            <p className="directory-subtitle">
              Discover coworkers and direct reports you can collaborate with.
            </p>
          </div>
          <div className="directory-header__actions">
            <button
              type="button"
              className="directory-back__button"
              onClick={() => navigate('/')}
            >
              Back
            </button>
          </div>
        </div>
      </header>

      <section className="directory-filters">
        <form onSubmit={handleSubmit}>
          <div className="directory-filters__grid">
            <label className="directory-field">
              <span>Search</span>
              <input
                type="text"
                placeholder="Name, email, employee ID..."
                value={formState.search}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, search: event.target.value }))
                }
              />
            </label>

            <label className="directory-field">
              <span>Department</span>
              <select
                value={formState.department}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, department: event.target.value }))
                }
              >
                <option value="">All departments</option>
                {departments.map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="directory-filters__actions">
            <button type="button" className="btn-secondary" onClick={resetFilters}>
              Reset
            </button>
            <button type="submit" className="btn-primary">
              Apply Filters
            </button>
          </div>
        </form>
      </section>

      {error && <div className="directory-error">{error}</div>}

      {loading ? (
        <div className="directory-loading">Loading coworkers...</div>
      ) : coworkers.length === 0 ? (
        <div className="directory-empty">
          <p>No teammates match these filters.</p>
          <button className="btn-secondary" onClick={resetFilters}>
            Clear filters
          </button>
        </div>
      ) : (
        <section className="directory-grid">
          {coworkers.map((person) => {
            const displayName = person.preferredName || person.legalFirstName || 'Employee';
            const shouldShowPill = person.directReport;
            const relationshipLabel = person.directReport ? 'Direct Report' : '';

            return (
              <article key={person.userId} className="directory-card">
                <div className="directory-card__avatar">
                  {displayName
                    .split(' ')
                    .map((word) => word[0])
                    .join('')
                    .slice(0, 2)
                    .toUpperCase()}
                </div>
                <div className="directory-card__body">
                  <div className="directory-card__title">
                    <h2>{displayName}</h2>
                    <span
                      className={`directory-pill ${
                        person.directReport ? 'pill-direct-report' : 'pill-coworker'
                      }`}
                    >
                      {relationshipLabel}
                    </span>
                  </div>
                  <p className="directory-card__job">
                    {person.jobTitle ?? 'Team member'} · {person.department ?? 'N/A'}
                  </p>
                  <p className="directory-card__meta">
                    Employee ID: <strong>{person.employeeId}</strong>
                    {person.workLocationType && (
                      <>
                        {' '}
                        · Location:{' '}
                        <strong>{person.workLocationType.toLowerCase()}</strong>
                      </>
                    )}
                  </p>
                  <div className="directory-card__actions">
                    <button
                      className="btn-secondary"
                      onClick={() => navigate(`/profile/${person.userId}`)}
                    >
                      View Profile
                    </button>
                    <button
                      className="btn-primary"
                      onClick={() => setRecipient(person)}
                    >
                      Give Feedback
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </section>
      )}

      {recipient && (
        <FeedbackModal
          recipientId={recipient.userId}
          recipientDisplayName={
            recipient.preferredName ||
            `${recipient.legalFirstName ?? ''} ${recipient.legalLastName ?? ''}`.trim()
          }
          isOpen={Boolean(recipient)}
          onClose={() => setRecipient(null)}
        />
      )}
    </div>
  );
}
