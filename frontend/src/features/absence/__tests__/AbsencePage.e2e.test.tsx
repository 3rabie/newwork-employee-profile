import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AbsencePage } from '../pages/AbsencePage';

vi.mock('../api/absenceApi', () => ({
  getMyAbsenceRequests: vi.fn().mockResolvedValue([
    {
      id: 'mine-1',
      userId: 'user-1',
      managerId: 'mgr-1',
      startDate: '2025-12-20',
      endDate: '2025-12-22',
      type: 'VACATION',
      status: 'PENDING',
      note: 'Test PTO'
    }
  ]),
  getPendingAbsenceRequests: vi.fn().mockResolvedValue([
    {
      id: 'pending-1',
      userId: 'report-1',
      managerId: 'mgr-1',
      startDate: '2025-12-10',
      endDate: '2025-12-12',
      type: 'SICK',
      status: 'PENDING',
      note: 'Sick leave'
    }
  ]),
  submitAbsenceRequest: vi.fn().mockResolvedValue({}),
  updateAbsenceStatus: vi.fn().mockResolvedValue({})
}));

vi.mock('../../auth/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      userId: 'mgr-1',
      email: 'manager@test.com',
      role: 'MANAGER',
      employeeId: 'MGR-001'
    }
  })
}));

const absenceApi = await import('../api/absenceApi');
vi.mock('../../../lib/graphql-client', () => ({
  graphqlRequest: vi.fn().mockResolvedValue({
    coworkerDirectory: [
      { userId: 'report-1', preferredName: 'Report', legalFirstName: 'Report', legalLastName: 'One', employeeId: 'EMP-200' }
    ]
  })
}));

// Provide prompt to avoid unhandled JSDOM errors
if (!(global as any).prompt) {
  (global as any).prompt = vi.fn();
}

describe('AbsencePage E2E-ish flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders team view pending list and approves', async () => {
    render(
      <MemoryRouter>
        <AbsencePage />
      </MemoryRouter>
    );

    expect(
      await screen.findByRole('heading', { name: /pending approvals/i })
    ).toBeInTheDocument();
    expect(screen.getByText(/Report One · EMP-200/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /approve/i }));

    await waitFor(() =>
      expect(absenceApi.updateAbsenceStatus).toHaveBeenCalledWith('pending-1', {
        action: 'APPROVE',
        note: undefined
      })
    );
  });
});
