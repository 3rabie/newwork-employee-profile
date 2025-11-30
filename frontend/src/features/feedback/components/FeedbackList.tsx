import type { FeedbackListItem } from '../types';
import './FeedbackList.css';

interface FeedbackListProps {
  items: FeedbackListItem[];
  isLoading?: boolean;
  emptyState?: string;
}

const formatName = (participant: FeedbackListItem['author']) => {
  return (
    participant.preferredName ||
    [participant.legalFirstName, participant.legalLastName].filter(Boolean).join(' ') ||
    participant.email
  );
};

const formatDateTime = (date: string) => {
  const value = new Date(date);
  return {
    date: value.toLocaleDateString(undefined, { dateStyle: 'medium' }),
    time: value.toLocaleTimeString(undefined, { timeStyle: 'short' }),
  };
};

export function FeedbackList({
  items,
  isLoading = false,
  emptyState = 'No feedback available yet.',
}: FeedbackListProps) {
  if (isLoading) {
    return <div className="feedback-list feedback-list--loading">Loading feedback...</div>;
  }

  if (!items.length) {
    return <div className="feedback-list feedback-list--empty">{emptyState}</div>;
  }

  return (
    <div className="feedback-list">
      {items.map((item) => {
        const formatted = formatDateTime(item.createdAt);
        return (
          <article className="feedback-card" key={item.id}>
            <header className="feedback-card__header">
              <div>
                <p className="feedback-card__author">{formatName(item.author)}</p>
                <p className="feedback-card__timestamp">
                  <span>{formatted.date}</span>
                  <span>•</span>
                  <span>{formatted.time}</span>
                </p>
              </div>
              {item.aiPolished && <span className="feedback-card__badge">AI polished</span>}
            </header>
            <p className="feedback-card__text">{item.text}</p>
          </article>
        );
      })}
    </div>
  );
}
