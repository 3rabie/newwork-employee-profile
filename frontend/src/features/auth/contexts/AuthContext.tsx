import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { authService } from '../api/auth-service';
import type { User, AuthContextType } from '../types';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restore auth state from sessionStorage on mount
  useEffect(() => {
    const storedToken = sessionStorage.getItem('auth_token');
    const storedUser = sessionStorage.getItem('auth_user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string): Promise<void> => {
    try {
      const response = await authService.login({ email, password });

      const userData: User = {
        userId: response.userId,
        email: response.email,
        employeeId: response.employeeId,
        role: response.role as 'EMPLOYEE' | 'MANAGER',
        managerId: response.managerId,
      };

      // Store auth data
      sessionStorage.setItem('auth_token', response.token);
      sessionStorage.setItem('auth_user', JSON.stringify(userData));

      setToken(response.token);
      setUser(userData);
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const switchUser = async (email: string): Promise<void> => {
    try {
      const response = await authService.switchUser({ email });

      const userData: User = {
        userId: response.userId,
        email: response.email,
        employeeId: response.employeeId,
        role: response.role as 'EMPLOYEE' | 'MANAGER',
        managerId: response.managerId,
      };

      // Update auth data
      sessionStorage.setItem('auth_token', response.token);
      sessionStorage.setItem('auth_user', JSON.stringify(userData));

      setToken(response.token);
      setUser(userData);
    } catch (error) {
      console.error('Switch user failed:', error);
      throw error;
    }
  };

  const logout = (): void => {
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('auth_user');
    setToken(null);
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: !!user && !!token,
    isLoading,
    login,
    switchUser,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
