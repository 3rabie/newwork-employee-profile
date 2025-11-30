import { httpClient } from './http-client';

export interface GraphQLError {
  message: string;
  path?: readonly (string | number)[];
  extensions?: Record<string, unknown>;
}

type GraphQLResponse<T> = {
  data?: T;
  errors?: GraphQLError[];
};

export class GraphQLRequestError extends Error {
  public readonly errors: GraphQLError[];

  constructor(message: string, errors: GraphQLError[]) {
    super(message);
    this.name = 'GraphQLRequestError';
    this.errors = errors;
  }
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
    throw new GraphQLRequestError(message || 'GraphQL request failed', response.data.errors);
  }

  if (!response.data.data) {
    throw new GraphQLRequestError('GraphQL response did not contain data', []);
  }

  return response.data.data;
}
