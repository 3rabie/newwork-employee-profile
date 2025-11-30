import { useState } from 'react';
import { createFeedback } from '../api/feedbackApi';
import { polishFeedback } from '../api/polishFeedback';
import type { FeedbackListItem } from '../types';
import './FeedbackModal.css';

const MIN_LENGTH = 10;

interface FeedbackModalProps {
  recipientId: string;
  recipientDisplayName: string;
  isOpen: boolean;
  onClose: () => void;
  onCreated?: (feedback: FeedbackListItem) => void;
}

export function FeedbackModal({
  recipientId,
  recipientDisplayName,
  isOpen,
  onClose,
  onCreated,
}: FeedbackModalProps) {
  const [text, setText] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPolishing, setIsPolishing] = useState(false);
  const [polishResult, setPolishResult] = useState<{
    originalText: string;
    polishedText: string;
  } | null>(null);
  const [usedPolish, setUsedPolish] = useState(false);

  if (!isOpen) {
    return null;
  }

  const resetState = () => {
    setText('');
    setError(null);
    setIsSubmitting(false);
    setIsPolishing(false);
    setPolishResult(null);
    setUsedPolish(false);
  };

  const closeModal = () => {
    resetState();
    onClose();
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (text.trim().length < MIN_LENGTH) {
      setError(`Feedback must be at least ${MIN_LENGTH} characters.`);
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      const feedback = await createFeedback({
        recipientId,
        text: text.trim(),
        aiPolished: usedPolish,
      });
      onCreated?.(feedback);
      closeModal();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to submit feedback.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePolish = async () => {
    if (text.trim().length < MIN_LENGTH) {
      setError(`Enter at least ${MIN_LENGTH} characters before polishing.`);
      return;
    }
    try {
      setIsPolishing(true);
      setError(null);
      const response = await polishFeedback(text.trim());
      setPolishResult(response);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to polish feedback.'
      );
      setPolishResult(null);
    } finally {
      setIsPolishing(false);
    }
  };

  const applyPolished = () => {
    if (!polishResult) return;
    setText(polishResult.polishedText);
    setUsedPolish(true);
    setPolishResult(null);
  };

  const keepOriginal = () => {
    setUsedPolish(false);
    setPolishResult(null);
  };

  return (
    <div className="feedback-modal__overlay" role="dialog" aria-modal="true">
      <div className="feedback-modal">
        <header className="feedback-modal__header">
          <div>
            <h2>Give Feedback</h2>
            <p>
              Recipient: <strong>{recipientDisplayName}</strong>
            </p>
          </div>
          <button className="btn-icon" onClick={closeModal} aria-label="Close modal">
            ×
          </button>
        </header>

        <form onSubmit={handleSubmit} className="feedback-modal__form">
          <label htmlFor="feedback-text">Feedback</label>
          <textarea
            id="feedback-text"
            value={text}
            onChange={(event) => {
              setText(event.target.value);
              setError(null);
            }}
            placeholder="Share actionable, specific feedback..."
            rows={5}
          />
          <div className="feedback-modal__actions">
            <span className="feedback-modal__hint">
              {text.trim().length}/{MIN_LENGTH} characters
            </span>
            {text.trim().length >= MIN_LENGTH && (
              <button
                type="button"
                className="btn-secondary"
                onClick={handlePolish}
                disabled={isPolishing}
              >
                {isPolishing ? 'Polishing...' : 'Polish with AI'}
              </button>
            )}
          </div>

          {polishResult && (
            <div className="feedback-modal__comparison">
              <div>
                <h3>Original</h3>
                <p>{polishResult.originalText}</p>
              </div>
              <div>
                <h3>Suggested</h3>
                <p>{polishResult.polishedText}</p>
              </div>
              <div className="feedback-modal__comparison-actions">
                <button type="button" className="btn-primary" onClick={applyPolished}>
                  Use polished version
                </button>
                <button type="button" className="btn-secondary" onClick={keepOriginal}>
                  Keep editing original
                </button>
              </div>
            </div>
          )}

          {error && <div className="feedback-modal__error">{error}</div>}

          <footer className="feedback-modal__footer">
            <button
              type="button"
              className="btn-secondary"
              onClick={closeModal}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting || text.trim().length < MIN_LENGTH}
            >
              {isSubmitting ? 'Sending...' : 'Send Feedback'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

