import React, { createContext, useContext, useState } from 'react';
import { User } from '../../shared/types';
import { authApi, LoginPayload } from '../services/authApi';

interface AuthContextType {
  user: User | null;
  token: string | null;
  agencyIds: string[];
  isAuthenticated: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  logout: () => void;
  updateToken: (newToken: string, newAgencyIds?: string[]) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });

  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('token');
  });

  const [agencyIds, setAgencyIds] = useState<string[]>(() => {
    const saved = localStorage.getItem('agencyIds');
    return saved ? JSON.parse(saved) : [];
  });

  const login = async (payload: LoginPayload) => {
    const data = await authApi.login(payload);
    setUser(data.user);
    setToken(data.accessToken);
    setAgencyIds(data.agencyIds || []);

    localStorage.setItem('user', JSON.stringify(data.user));
    localStorage.setItem('token', data.accessToken);
    localStorage.setItem('agencyIds', JSON.stringify(data.agencyIds || []));
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    setAgencyIds([]);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('agencyIds');
    localStorage.removeItem('selectedAgencyId');
  };

  const updateToken = (newToken: string, newAgencyIds?: string[]) => {
    setToken(newToken);
    localStorage.setItem('token', newToken);
    if (newAgencyIds) {
      setAgencyIds(newAgencyIds);
      localStorage.setItem('agencyIds', JSON.stringify(newAgencyIds));
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        agencyIds,
        isAuthenticated: !!token && !!user,
        login,
        logout,
        updateToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
