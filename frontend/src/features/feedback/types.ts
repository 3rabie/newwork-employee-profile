export interface FeedbackParticipant {
  id: string;
  email: string;
  employeeId?: string;
  preferredName?: string;
  legalFirstName?: string;
  legalLastName?: string;
}

export interface FeedbackListItem {
  id: string;
  text: string;
  aiPolished: boolean;
  createdAt: string;
  author: FeedbackParticipant;
  recipient: FeedbackParticipant;
}

export interface CreateFeedbackRequestPayload {
  recipientId: string;
  text: string;
  aiPolished?: boolean;
}

