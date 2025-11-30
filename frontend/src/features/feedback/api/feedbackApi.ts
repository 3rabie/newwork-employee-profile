import { graphqlRequest } from '../../../lib/graphql-client';
import {
  GET_FEEDBACK_FOR_USER_QUERY,
  GET_MY_AUTHORED_FEEDBACK_QUERY,
  GET_MY_RECEIVED_FEEDBACK_QUERY,
} from '../../../lib/graphql-queries';
import { httpClient } from '../../../lib/http-client';
import type {
  CreateFeedbackRequestPayload,
  FeedbackListItem,
  FeedbackParticipant,
} from '../types';

type GraphQLFeedbackUser = {
  id: string;
  email: string;
  employeeId: string;
  profile?: {
    preferredName?: string;
    legalFirstName?: string;
    legalLastName?: string;
  } | null;
};

type GraphQLFeedbackNode = {
  id: string;
  text: string;
  aiPolished: boolean;
  createdAt: string;
  author: GraphQLFeedbackUser;
  recipient: GraphQLFeedbackUser;
};

const mapUser = (user: GraphQLFeedbackUser): FeedbackParticipant => {
  const preferredName =
    user.profile?.preferredName ||
    [user.profile?.legalFirstName, user.profile?.legalLastName]
      .filter(Boolean)
      .join(' ')
      .trim();

  return {
    id: user.id,
    email: user.email,
    employeeId: user.employeeId,
    preferredName: preferredName || undefined,
    legalFirstName: user.profile?.legalFirstName,
    legalLastName: user.profile?.legalLastName,
  };
};

const mapFeedback = (node: GraphQLFeedbackNode): FeedbackListItem => ({
  id: node.id,
  text: node.text,
  aiPolished: node.aiPolished,
  createdAt: node.createdAt,
  author: mapUser(node.author),
  recipient: mapUser(node.recipient),
});

export async function getFeedbackForUser(
  userId: string
): Promise<FeedbackListItem[]> {
  const data = await graphqlRequest<{ feedbackForUser: GraphQLFeedbackNode[] }>(
    GET_FEEDBACK_FOR_USER_QUERY,
    { userId },
    'GetFeedbackForUser'
  );
  return data.feedbackForUser.map(mapFeedback);
}

export async function getMyAuthoredFeedback(): Promise<FeedbackListItem[]> {
  const data = await graphqlRequest<{ myAuthoredFeedback: GraphQLFeedbackNode[] }>(
    GET_MY_AUTHORED_FEEDBACK_QUERY,
    undefined,
    'GetMyAuthoredFeedback'
  );
  return data.myAuthoredFeedback.map(mapFeedback);
}

export async function getMyReceivedFeedback(): Promise<FeedbackListItem[]> {
  const data = await graphqlRequest<{ myReceivedFeedback: GraphQLFeedbackNode[] }>(
    GET_MY_RECEIVED_FEEDBACK_QUERY,
    undefined,
    'GetMyReceivedFeedback'
  );
  return data.myReceivedFeedback.map(mapFeedback);
}

export async function createFeedback(
  payload: CreateFeedbackRequestPayload
): Promise<FeedbackListItem> {
  const response = await httpClient.post<{
    id: string;
    text: string;
    aiPolished: boolean;
    createdAt: string;
    authorId: string;
    authorName: string;
    recipientId: string;
    recipientName: string;
  }>('/api/feedback', payload);

  const dto = response.data;
  const buildParticipant = (
    id: string,
    displayName: string
  ): FeedbackParticipant => {
    const [first, ...rest] = displayName.split(' ');
    return {
      id,
      email: '',
      preferredName: displayName,
      legalFirstName: first,
      legalLastName: rest.join(' ') || undefined,
    };
  };

  return {
    id: dto.id,
    text: dto.text,
    aiPolished: Boolean(dto.aiPolished),
    createdAt: dto.createdAt,
    author: buildParticipant(dto.authorId, dto.authorName ?? 'Author'),
    recipient: buildParticipant(dto.recipientId, dto.recipientName ?? 'Recipient'),
  };
}

