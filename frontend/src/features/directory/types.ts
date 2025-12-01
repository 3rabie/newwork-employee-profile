export interface Coworker {
  userId: string;
  employeeId: string;
  preferredName?: string;
  legalFirstName: string;
  legalLastName: string;
  jobTitle?: string;
  department?: string;
  workLocationType?: string;
  profilePhotoUrl?: string;
  relationship: string;
  directReport: boolean;
  pendingAbsenceCount?: number | null;
}

export interface DirectoryFilters {
  search?: string;
  department?: string;
  directReportsOnly?: boolean;
}
