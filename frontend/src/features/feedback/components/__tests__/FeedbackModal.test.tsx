import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FeedbackModal } from '../FeedbackModal';
import type { FeedbackListItem } from '../../types';

const mockCreateFeedback = vi.fn();
const mockPolishFeedback = vi.fn();

vi.mock('../../api/feedbackApi', () => ({
  createFeedback: (...args: unknown[]) => mockCreateFeedback(...args),
}));

vi.mock('../../api/polishFeedback', () => ({
  polishFeedback: (...args: unknown[]) => mockPolishFeedback(...args),
}));

const baseFeedback: FeedbackListItem = {
  id: 'fb-123',
  text: 'Great teamwork!',
  aiPolished: false,
  createdAt: '2024-01-01T00:00:00Z',
  author: { id: 'author-1', email: 'author@test.com', preferredName: 'Author' },
  recipient: { id: 'recipient-1', email: 'recipient@test.com', preferredName: 'Recipient' },
};

describe('FeedbackModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCreateFeedback.mockResolvedValue(baseFeedback);
    mockPolishFeedback.mockResolvedValue({
      originalText: 'Great teamwork everyone!',
      polishedText: 'Great teamwork everyone! Keep the collaboration going.',
    });
  });

  it('submits feedback and notifies parent', async () => {
    const onCreated = vi.fn();
    const user = userEvent.setup();

    render(
      <FeedbackModal
        recipientId="recipient-1"
        recipientDisplayName="Recipient"
        isOpen
        onClose={() => undefined}
        onCreated={onCreated}
      />
    );

    const textArea = screen.getByPlaceholderText(/actionable/i);
    await user.type(textArea, 'Great teamwork everyone!');

    await user.click(screen.getByRole('button', { name: /send feedback/i }));

    await waitFor(() => expect(mockCreateFeedback).toHaveBeenCalled());
    expect(onCreated).toHaveBeenCalledWith(baseFeedback);
  });

  it('shows AI polish suggestion and allows applying it', async () => {
    const user = userEvent.setup();
    render(
      <FeedbackModal
        recipientId="recipient-1"
        recipientDisplayName="Recipient"
        isOpen
        onClose={() => undefined}
      />
    );

    const textArea = screen.getByPlaceholderText(/actionable/i);
    await user.type(textArea, 'Great teamwork everyone!');
    await user.click(screen.getByRole('button', { name: /polish with ai/i }));

    await waitFor(() =>
      expect(
        screen.getByText(/keep the collaboration going/i)
      ).toBeInTheDocument()
    );

    await user.click(screen.getByRole('button', { name: /use polished version/i }));
    expect((textArea as HTMLTextAreaElement).value).toContain('Keep the collaboration going');
  });
});
