import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AbsencePage } from '../pages/AbsencePage';

vi.mock('../api/absenceApi', () => ({
  getMyAbsenceRequests: vi.fn(),
  getPendingAbsenceRequests: vi.fn(),
  submitAbsenceRequest: vi.fn(),
  updateAbsenceStatus: vi.fn()
}));

vi.mock('../../auth/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      userId: 'user-1',
      email: 'user@test.com',
      role: 'MANAGER',
      employeeId: 'EMP-1'
    }
  })
}));

const absenceApi = await import('../api/absenceApi');
vi.mock('../../../lib/graphql-client', () => ({
  graphqlRequest: vi.fn().mockResolvedValue({ coworkerDirectory: [] })
}));

// Vitest/JSDOM: provide prompt to avoid unhandled crashes
if (!(global as any).prompt) {
  (global as any).prompt = vi.fn();
}

describe('AbsencePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('validates dates before submit', async () => {
    (absenceApi.getMyAbsenceRequests as vi.Mock).mockResolvedValue([]);
    (absenceApi.getPendingAbsenceRequests as vi.Mock).mockResolvedValue([]);
    render(
      <MemoryRouter>
        <AbsencePage />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /submit request/i }));

    expect(await screen.findByText(/start date is required/i)).toBeInTheDocument();
    expect(screen.getByText(/end date is required/i)).toBeInTheDocument();
    expect(absenceApi.submitAbsenceRequest).not.toHaveBeenCalled();
  });

  it('submits with future dates and reloads lists', async () => {
    (absenceApi.getMyAbsenceRequests as vi.Mock).mockResolvedValue([]);
    (absenceApi.getPendingAbsenceRequests as vi.Mock).mockResolvedValue([]);
    (absenceApi.submitAbsenceRequest as vi.Mock).mockResolvedValue({});

    render(
      <MemoryRouter>
        <AbsencePage />
      </MemoryRouter>
    );

    const today = new Date();
    const start = new Date(today);
    start.setDate(start.getDate() + 1);
    const end = new Date(today);
    end.setDate(end.getDate() + 2);

    fireEvent.change(screen.getByLabelText(/start/i), {
      target: { value: start.toISOString().slice(0, 10) }
    });
    fireEvent.change(screen.getByLabelText(/end/i), {
      target: { value: end.toISOString().slice(0, 10) }
    });

    fireEvent.click(screen.getByRole('button', { name: /submit request/i }));

    await waitFor(() => expect(absenceApi.submitAbsenceRequest).toHaveBeenCalled());
    expect(absenceApi.getMyAbsenceRequests).toHaveBeenCalledTimes(2); // initial + reload
  });

  it('allows past dates for sick leave', async () => {
    (absenceApi.getMyAbsenceRequests as vi.Mock).mockResolvedValue([]);
    (absenceApi.getPendingAbsenceRequests as vi.Mock).mockResolvedValue([]);
    (absenceApi.submitAbsenceRequest as vi.Mock).mockResolvedValue({});

    render(
      <MemoryRouter>
        <AbsencePage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/type/i), { target: { value: 'SICK' } });
    fireEvent.change(screen.getByLabelText(/start/i), {
      target: { value: '2024-01-01' }
    });
    fireEvent.change(screen.getByLabelText(/end/i), {
      target: { value: '2024-01-02' }
    });

    fireEvent.click(screen.getByRole('button', { name: /submit request/i }));

    await waitFor(() => expect(absenceApi.submitAbsenceRequest).toHaveBeenCalled());
  });

  it('shows pending approvals for managers and calls update on approve', async () => {
    (absenceApi.getMyAbsenceRequests as vi.Mock).mockResolvedValue([]);
    (absenceApi.getPendingAbsenceRequests as vi.Mock).mockResolvedValue([
      {
        id: 'abs-1',
        userId: 'emp-2',
        managerId: 'mgr-1',
        startDate: '2025-12-10',
        endDate: '2025-12-12',
        type: 'VACATION',
        status: 'PENDING',
        note: 'Test'
      }
    ]);
    (absenceApi.updateAbsenceStatus as vi.Mock).mockResolvedValue({});

    render(
      <MemoryRouter>
        <AbsencePage />
      </MemoryRouter>
    );

    expect(
      await screen.findByRole('heading', { name: /pending approvals/i })
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /approve/i }));

    await waitFor(() =>
      expect(absenceApi.updateAbsenceStatus).toHaveBeenCalledWith('abs-1', {
        action: 'APPROVE',
        note: undefined
      })
    );
  });
});
