import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '../../../lib/http-client';
import { polishFeedback } from './polishFeedback';

vi.mock('../../../lib/http-client', () => {
  return {
    httpClient: {
      post: vi.fn(),
    },
  };
});

describe('polishFeedback', () => {
  it('sends POST request and returns polished payload', async () => {
    (httpClient.post as unknown as vi.Mock).mockResolvedValue({
      data: {
        originalText: 'Great teamwork everyone!',
        polishedText: 'Great teamwork everyone! Keep the momentum going.',
      },
    });

    const result = await polishFeedback('Great teamwork everyone!');

    expect(httpClient.post).toHaveBeenCalledWith('/api/feedback/polish', {
      text: 'Great teamwork everyone!',
    });
    expect(result.polishedText).toContain('momentum');
  });
});
