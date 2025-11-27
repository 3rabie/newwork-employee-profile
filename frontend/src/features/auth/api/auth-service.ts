import { httpClient } from '../../../lib/http-client';
import type { AuthResponse, LoginRequest, SwitchUserRequest } from '../types';

export const authService = {
  /**
   * Authenticate user with email and password
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await httpClient.post<AuthResponse>('/api/auth/login', credentials);
    return response.data;
  },

  /**
   * Switch to a different user (demo feature for testing roles)
   */
  switchUser: async (request: SwitchUserRequest): Promise<AuthResponse> => {
    const response = await httpClient.post<AuthResponse>('/api/auth/switch-user', request);
    return response.data;
  },
};
