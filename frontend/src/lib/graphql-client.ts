import { GraphQLClient } from 'graphql-request';

const GRAPHQL_ENDPOINT = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/graphql`;

/**
 * GraphQL client configured with authentication
 */
export const graphqlClient = new GraphQLClient(GRAPHQL_ENDPOINT, {
  headers: {},
  requestMiddleware: (request) => {
    const authToken = sessionStorage.getItem('auth_token');
    if (authToken) {
      return {
        ...request,
        headers: {
          ...request.headers,
          Authorization: `Bearer ${authToken}`,
        },
      };
    }
    return request;
  },
  responseMiddleware: (response) => {
    // Handle unauthorized errors
    if (response instanceof Error) {
      // Check if it's a GraphQL error with unauthorized status
      const errorMessage = response.message.toLowerCase();
      if (errorMessage.includes('unauthorized') || errorMessage.includes('401')) {
        sessionStorage.removeItem('auth_token');
        sessionStorage.removeItem('auth_user');
        window.location.href = '/login';
      }
    }
  },
});
