import { httpClient } from './http-client';

interface GraphQLError {
  message: string;
  path?: readonly (string | number)[];
  extensions?: Record<string, unknown>;
}

interface GraphQLResponse<T> {
  data?: T;
  errors?: GraphQLError[];
}

/**
 * Minimal GraphQL helper that reuses the shared Axios client so headers/interceptors stay consistent.
 */
export async function graphqlRequest<TData>(
  query: string,
  variables?: Record<string, unknown>,
  operationName?: string
): Promise<TData> {
  const response = await httpClient.post<GraphQLResponse<TData>>('/graphql', {
    query,
    variables,
    operationName
  });

  if (response.data.errors?.length) {
    const message = response.data.errors.map((error) => error.message).join('; ');
    throw new Error(message || 'GraphQL request failed');
  }

  if (!response.data.data) {
    throw new Error('GraphQL response did not contain data');
  }

  return response.data.data;
}
