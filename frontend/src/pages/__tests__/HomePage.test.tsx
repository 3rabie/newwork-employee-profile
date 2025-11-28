import { MemoryRouter } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { HomePage } from '../HomePage';

const mockUseAuth = vi.fn();
const mockNavigate = vi.fn();

vi.mock('../../features/auth/contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../features/auth/components/SwitchUserDialog', () => ({
  SwitchUserDialog: ({ isOpen }: { isOpen: boolean }) =>
    isOpen ? <div data-testid="switch-dialog">Switch Dialog</div> : null,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom'
  );
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('HomePage', () => {
  const baseUser = {
    userId: 'user-123',
    email: 'test.user@company.com',
    employeeId: 'EMP123',
    role: 'EMPLOYEE' as const,
    managerId: null,
  };

  beforeEach(() => {
    mockUseAuth.mockReset();
    mockNavigate.mockReset();
  });

  it('displays the current user details', () => {
    mockUseAuth.mockReturnValue({
      user: baseUser,
      logout: vi.fn(),
    });

    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

    expect(screen.getByText('test.user@company.com')).toBeInTheDocument();
    expect(screen.getAllByText(/EMPLOYEE/i).length).toBeGreaterThan(0);
    expect(screen.getByText('EMP123')).toBeInTheDocument();
  });

  it('navigates to profile when clicking View My Profile', async () => {
    mockUseAuth.mockReturnValue({
      user: baseUser,
      logout: vi.fn(),
    });

    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /view my profile/i }));

    expect(mockNavigate).toHaveBeenCalledWith('/profile/user-123');
  });

  it('calls logout handler when Logout button clicked', async () => {
    const logout = vi.fn();
    mockUseAuth.mockReturnValue({
      user: baseUser,
      logout,
    });

    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /logout/i }));

    expect(logout).toHaveBeenCalled();
  });
});
