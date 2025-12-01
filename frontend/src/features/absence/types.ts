export type AbsenceType = 'VACATION' | 'SICK' | 'PERSONAL';

export type AbsenceStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export type AbsenceRequest = {
  id: string;
  userId: string;
  managerId?: string | null;
  startDate: string;
  endDate: string;
  type: AbsenceType;
  status: AbsenceStatus;
  note?: string | null;
};

export type CreateAbsenceRequestInput = {
  startDate: string;
  endDate: string;
  type: AbsenceType;
  note?: string;
};

export type UpdateAbsenceStatusInput = {
  action: 'APPROVE' | 'REJECT';
  note?: string;
};
