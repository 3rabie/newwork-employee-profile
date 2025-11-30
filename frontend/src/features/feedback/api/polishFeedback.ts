import { httpClient } from '../../../lib/http-client';

export interface PolishFeedbackResponse {
  originalText: string;
  polishedText: string;
}

export const polishFeedback = async (
  text: string
): Promise<PolishFeedbackResponse> => {
  const response = await httpClient.post<PolishFeedbackResponse>(
    '/api/feedback/polish',
    { text }
  );
  return response.data;
};
