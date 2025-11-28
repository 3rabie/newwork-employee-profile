/**
 * Profile API Service
 *
 * API functions for employee profile management.
 * Handles GET and PATCH requests to /api/profiles endpoints.
 */

import { httpClient } from '../../../lib/http-client';
import type { ProfileDTO, ProfileUpdateDTO } from '../types';

/**
 * Get employee profile by user ID.
 * Returns profile data filtered by viewer's permissions.
 *
 * @param userId - The UUID of the profile owner
 * @returns Promise resolving to ProfileDTO
 * @throws Error if profile not found or forbidden
 */
export const getProfile = async (userId: string): Promise<ProfileDTO> => {
  const response = await httpClient.get<ProfileDTO>(`/api/profiles/${userId}`);
  return response.data;
};

/**
 * Update employee profile.
 * Only updates fields the viewer has permission to edit.
 *
 * @param userId - The UUID of the profile owner
 * @param updates - Partial profile updates
 * @returns Promise resolving to updated ProfileDTO
 * @throws Error if profile not found, forbidden, or validation fails
 */
export const updateProfile = async (
  userId: string,
  updates: ProfileUpdateDTO
): Promise<ProfileDTO> => {
  const response = await httpClient.patch<ProfileDTO>(
    `/api/profiles/${userId}`,
    updates
  );
  return response.data;
};
