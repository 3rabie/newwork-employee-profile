import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import ProfilePage from '../ProfilePage';
import { AuthProvider } from '../../../auth/contexts/AuthContext';
import { ProtectedRoute } from '../../../auth/components/ProtectedRoute';
import { httpClient } from '../../../../lib/http-client';
import type { ProfileDTO } from '../../types';
import type { FeedbackListItem } from '../../../feedback/types';

const mockGetFeedbackForUser = vi.fn();
const mockCreateFeedback = vi.fn();

vi.mock('../../../feedback/api/feedbackApi', () => ({
  getFeedbackForUser: (...args: unknown[]) => mockGetFeedbackForUser(...args),
  createFeedback: (...args: unknown[]) => mockCreateFeedback(...args),
}));

const mockPolishFeedback = vi.fn();
vi.mock('../../../feedback/api/polishFeedback', () => ({
  polishFeedback: (...args: unknown[]) => mockPolishFeedback(...args),
}));

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
  metadata: {
    relationship: 'SELF',
    visibleFields: ['SYSTEM_MANAGED', 'NON_SENSITIVE', 'SENSITIVE'],
    editableFields: ['NON_SENSITIVE', 'SENSITIVE'],
  },
};

const sampleFeedback: FeedbackListItem = {
  id: 'fb-1',
  text: 'Great teamwork everyone!',
  aiPolished: true,
  createdAt: '2024-02-01T10:00:00Z',
  author: {
    id: 'user-2',
    email: 'coworker@test.com',
    preferredName: 'Coworker',
  },
  recipient: {
    id: 'user-1',
    email: 'test.person@company.com',
    preferredName: 'Test Person',
  },
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
    mockGetFeedbackForUser.mockResolvedValue([sampleFeedback]);
    mockCreateFeedback.mockResolvedValue(sampleFeedback);
    mockPolishFeedback.mockResolvedValue({
      originalText: 'Team is doing well',
      polishedText: 'Team is doing well and I appreciate the collaboration.',
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

  it('allows giving AI-polished feedback to another user', async () => {
    sessionStorage.setItem(
      'auth_user',
      JSON.stringify({
        userId: 'user-2',
        email: 'coworker@test.com',
        employeeId: 'EMP-002',
        role: 'EMPLOYEE',
        managerId: 'user-1',
      })
    );

    renderApp();

    await screen.findByRole('heading', { name: /feedback/i });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /give feedback/i }));

    const textArea = screen.getByPlaceholderText(/share actionable/i);
    await user.type(textArea, 'Team is doing well');

    await user.click(screen.getByRole('button', { name: /polish with ai/i }));
    await waitFor(() =>
      expect(
        screen.getByText(/team is doing well and i appreciate/i)
      ).toBeInTheDocument()
    );

    await user.click(screen.getByRole('button', { name: /use polished version/i }));
    await user.click(screen.getByRole('button', { name: /send feedback/i }));

    await waitFor(() => {
      expect(mockCreateFeedback).toHaveBeenCalledWith({
        recipientId: 'user-1',
        text: 'Team is doing well and I appreciate the collaboration.',
        aiPolished: true,
      });
    });
  });
});
