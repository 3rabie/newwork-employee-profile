import { graphqlRequest } from '../../../lib/graphql-client';
import { GET_COWORKER_DIRECTORY_QUERY } from '../../../lib/graphql-queries';
import type { Coworker, DirectoryFilters } from '../types';

interface CoworkerDirectoryResponse {
  coworkerDirectory: Coworker[];
}

/**
 * Fetch coworker directory entries with optional filters.
 */
export async function getCoworkerDirectory(filters?: DirectoryFilters) {
  const variables: Record<string, string | boolean> = {};
  if (filters?.search) {
    variables.search = filters.search;
  }
  if (filters?.department) {
    variables.department = filters.department;
  }
  if (filters?.directReportsOnly) {
    variables.directReportsOnly = true;
  }

  const data = await graphqlRequest<CoworkerDirectoryResponse>(
    GET_COWORKER_DIRECTORY_QUERY,
    Object.keys(variables).length ? variables : undefined,
    'GetCoworkerDirectory'
  );
  return data.coworkerDirectory;
}
