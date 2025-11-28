import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../contexts/AuthContext';
import './SwitchUserDialog.css';

interface SwitchUserDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export function SwitchUserDialog({ isOpen, onClose }: SwitchUserDialogProps) {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { switchUser } = useAuth();

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');

    if (!email) {
      setError('Email is required');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setError('Please enter a valid email address');
      return;
    }

    setIsSubmitting(true);

    try {
      await switchUser(email);
      setEmail('');
      onClose();
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosError = err as { response?: { status: number } };
        if (axiosError.response?.status === 404) {
          setError('User not found with this email');
        } else {
          setError('An error occurred. Please try again.');
        }
      } else {
        setError('An error occurred. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSelectDemoUser = (demoEmail: string) => {
    setEmail(demoEmail);
  };

  if (!isOpen) return null;

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <h2>Switch User</h2>
          <button
            className="dialog-close"
            onClick={onClose}
            aria-label="Close dialog"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="dialog-form">
          {error && (
            <div className="error-message" role="alert">
              {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="switch-email">User Email</label>
            <input
              type="email"
              id="switch-email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@company.com"
              disabled={isSubmitting}
              autoComplete="email"
              required
            />
          </div>

          <div className="demo-users">
            <p className="demo-users-label">Quick Switch:</p>
            <div className="demo-user-buttons">
              <button
                type="button"
                className="btn-demo-user"
                onClick={() => handleSelectDemoUser('manager@company.com')}
                disabled={isSubmitting}
              >
                Manager
              </button>
              <button
                type="button"
                className="btn-demo-user"
                onClick={() => handleSelectDemoUser('emp1@company.com')}
                disabled={isSubmitting}
              >
                Employee 1
              </button>
              <button
                type="button"
                className="btn-demo-user"
                onClick={() => handleSelectDemoUser('emp2@company.com')}
                disabled={isSubmitting}
              >
                Employee 2
              </button>
            </div>
          </div>

          <div className="dialog-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Switching...' : 'Switch User'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
