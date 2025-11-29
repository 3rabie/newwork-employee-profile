import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import ProfilePage from '../ProfilePage';
import { AuthProvider } from '../../../auth/contexts/AuthContext';
import { ProtectedRoute } from '../../../auth/components/ProtectedRoute';
import { httpClient } from '../../../../lib/http-client';
import type { ProfileDTO } from '../../types';

const mockProfile: ProfileDTO = {
  id: 'profile-1',
  userId: 'user-1',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  legalFirstName: 'Test',
  legalLastName: 'Person',
  email: 'test.person@company.com',
  employeeId: 'EMP-001',
  department: 'Engineering',
  jobCode: 'ENG',
  jobFamily: 'Engineering',
  jobLevel: 'Senior',
  employmentStatus: 'ACTIVE',
  hireDate: '2020-01-01',
  terminationDate: null,
  fte: 1,
  preferredName: 'Test Person',
  jobTitle: 'Engineering Manager',
  officeLocation: 'Remote',
  workPhone: '+1-555-0100',
  workLocationType: 'REMOTE',
  bio: 'Hi there',
  skills: 'Leadership',
  profilePhotoUrl: 'https://example.com/photo.jpg',
  personalEmail: 'personal@example.com',
  personalPhone: '+1-555-9999',
  homeAddress: '123 Anywhere St',
  emergencyContactName: 'Emergency Contact',
  emergencyContactPhone: '+1-555-0000',
  emergencyContactRelationship: 'Partner',
  dateOfBirth: '1990-01-01',
  visaWorkPermit: 'Citizen',
  absenceBalanceDays: 10,
  salary: 100000,
  performanceRating: 'EXCEEDS',
};

describe('ProfilePage E2E flow', () => {
  const httpPostSpy = vi.spyOn(httpClient, 'post');
  const httpPatchSpy = vi.spyOn(httpClient, 'patch');

  const seedAuthSession = () => {
    sessionStorage.setItem('auth_token', 'test-token');
    sessionStorage.setItem(
      'auth_user',
      JSON.stringify({
        userId: 'user-1',
        email: 'test.person@company.com',
        employeeId: 'EMP-001',
        role: 'EMPLOYEE',
        managerId: null,
      })
    );
  };

  const renderApp = () =>
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/profile/user-1']}>
          <Routes>
            <Route
              path="/profile/:userId"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    );

  beforeEach(() => {
    vi.useRealTimers();
    sessionStorage.clear();
    seedAuthSession();
    httpPostSpy.mockResolvedValue({
      data: { data: { profile: mockProfile } },
    });
    httpPatchSpy.mockResolvedValue({
      data: { ...mockProfile, jobTitle: 'Director of Engineering' },
    });
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('allows editing profile fields end-to-end', async () => {
    renderApp();

    await screen.findByText('Basic Information');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /edit profile/i }));

    const jobTitleInput = screen.getByPlaceholderText('Enter job title');
    await user.clear(jobTitleInput);
    await user.type(jobTitleInput, 'Director of Engineering');

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(httpPatchSpy).toHaveBeenCalledTimes(1);
    });

    const patchPayload = httpPatchSpy.mock.calls[0][1];
    expect(patchPayload).toEqual({ jobTitle: 'Director of Engineering' });

    await waitFor(() =>
      expect(
        screen.getAllByText('Director of Engineering').length
      ).toBeGreaterThan(0)
    );
  });
});
