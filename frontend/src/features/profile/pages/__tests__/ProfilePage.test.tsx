import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, vi } from 'vitest';
import ProfilePage from '../ProfilePage';
import type { ProfileDTO } from '../../types';
import { AuthProvider } from '../../../auth/contexts/AuthContext';

const mockGetProfile = vi.fn();
const mockUpdateProfile = vi.fn();
const mockGetFeedbackForUser = vi.fn();

vi.mock('../../api/profileApi', () => ({
  getProfile: (...args: unknown[]) => mockGetProfile(...args),
  updateProfile: (...args: unknown[]) => mockUpdateProfile(...args),
}));

vi.mock('../../../feedback/api/feedbackApi', () => ({
  getFeedbackForUser: (...args: unknown[]) => mockGetFeedbackForUser(...args),
}));

const renderProfilePage = () => {
  sessionStorage.setItem('auth_token', 'test-token');
  sessionStorage.setItem(
    'auth_user',
    JSON.stringify({
      userId: 'user-1',
      email: 'manager@test.com',
      employeeId: 'EMP-100',
      role: 'EMPLOYEE',
      managerId: null,
    })
  );
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/profile/user-1']}>
        <Routes>
          <Route path="/profile/:userId" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
};

const baseProfile: ProfileDTO = {
  id: 'profile-1',
  userId: 'user-1',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  legalFirstName: 'Test',
  legalLastName: 'User',
  department: 'Engineering',
  jobCode: 'ENG',
  jobFamily: 'Engineering',
  jobLevel: 'Senior',
  employmentStatus: 'ACTIVE',
  hireDate: '2020-01-01',
  terminationDate: null,
  fte: 1,
  preferredName: 'Test User',
  jobTitle: 'Engineering Manager',
  officeLocation: 'Remote',
  workPhone: '+1-555-0100',
  workLocationType: 'REMOTE',
  bio: 'Hello world',
  skills: 'Leadership',
  profilePhotoUrl: 'https://example.com',
  email: 'manager@test.com',
  employeeId: 'EMP-100',
  personalEmail: 'personal@test.com',
  personalPhone: '+1-555-9999',
  homeAddress: '123 Street',
  emergencyContactName: 'Contact',
  emergencyContactPhone: '+1-555-0000',
  emergencyContactRelationship: 'Spouse',
  dateOfBirth: '1990-01-01',
  visaWorkPermit: 'Citizen',
  absenceBalanceDays: 10,
  salary: 100000,
  performanceRating: 'Exceeds',
};

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetProfile.mockResolvedValue(baseProfile);
    mockUpdateProfile.mockResolvedValue(baseProfile);
    mockGetFeedbackForUser.mockResolvedValue([]);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('shows validation errors before saving invalid data', async () => {
    renderProfilePage();
    await screen.findByText('Basic Information');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /edit profile/i }));

    const emailInput = screen.getByPlaceholderText('personal@email.com');
    await user.clear(emailInput);
    await user.type(emailInput, 'invalid-email');

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(
      await screen.findByText(/enter a valid email address/i)
    ).toBeInTheDocument();
    expect(mockUpdateProfile).not.toHaveBeenCalled();
  });

  it('optimistically updates job title while saving and reverts on failure', async () => {
    renderProfilePage();
    await screen.findByText('Basic Information');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /edit profile/i }));

    const jobTitleInput = screen.getByPlaceholderText('Enter job title');
    await user.clear(jobTitleInput);
    await user.type(jobTitleInput, 'Director of Engineering');

    let rejectPromise: ((reason?: unknown) => void) | null = null;
    mockUpdateProfile.mockImplementationOnce(
      () =>
        new Promise((_resolve, reject) => {
          rejectPromise = reject;
        })
    );

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => expect(mockUpdateProfile).toHaveBeenCalled());
    expect(
      screen.getAllByText('Director of Engineering').length
    ).toBeGreaterThan(0);

    act(() => {
      rejectPromise?.(new Error('Failed'));
    });

    await waitFor(() =>
      expect(
        screen.getByText('Failed to save changes')
      ).toBeInTheDocument()
    );
    expect(screen.getByText('Engineering Manager')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save changes/i })).toBeEnabled();
  });
});
