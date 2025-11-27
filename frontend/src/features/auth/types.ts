export interface User {
  userId: string;
  email: string;
  employeeId: string;
  role: 'EMPLOYEE' | 'MANAGER';
  managerId: string | null;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  employeeId: string;
  role: string;
  managerId: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SwitchUserRequest {
  email: string;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  switchUser: (email: string) => Promise<void>;
  logout: () => void;
}
