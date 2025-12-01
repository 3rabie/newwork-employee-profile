import { graphqlRequest } from '../../../lib/graphql-client';
import { httpClient } from '../../../lib/http-client';
import {
  GET_MY_ABSENCES_QUERY,
  GET_PENDING_ABSENCES_QUERY
} from '../../../lib/graphql-queries';
import type {
  AbsenceRequest,
  CreateAbsenceRequestInput,
  UpdateAbsenceStatusInput
} from '../types';

type MyAbsenceResponse = {
  myAbsenceRequests: AbsenceRequest[];
};

type PendingAbsenceResponse = {
  pendingAbsenceRequests: AbsenceRequest[];
};

export async function getMyAbsenceRequests(): Promise<AbsenceRequest[]> {
  const data = await graphqlRequest<MyAbsenceResponse>(GET_MY_ABSENCES_QUERY);
  return data.myAbsenceRequests;
}

export async function getPendingAbsenceRequests(): Promise<AbsenceRequest[]> {
  const data = await graphqlRequest<PendingAbsenceResponse>(GET_PENDING_ABSENCES_QUERY);
  return data.pendingAbsenceRequests;
}

export async function submitAbsenceRequest(
  input: CreateAbsenceRequestInput
): Promise<AbsenceRequest> {
  const response = await httpClient.post<AbsenceRequest>('/api/absence', input);
  return response.data;
}

export async function updateAbsenceStatus(
  id: string,
  input: UpdateAbsenceStatusInput
): Promise<AbsenceRequest> {
  const response = await httpClient.patch<AbsenceRequest>(`/api/absence/${id}`, input);
  return response.data;
}
