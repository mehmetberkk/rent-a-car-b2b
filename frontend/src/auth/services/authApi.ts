import { apiClient } from '../../api/client';
import { User } from '../../shared/types';

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface AuthResponseData {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  agencyIds: string[];
}

export interface RefreshedTokenData {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  agencyIds: string[];
}

export const authApi = {
  register: async (payload: RegisterPayload) => {
    const res = await apiClient.post('/auth/register', payload);
    return res.data;
  },

  activate: async (token: string) => {
    const res = await apiClient.post('/auth/activate', { token });
    return res.data;
  },

  resendActivation: async (email: string) => {
    const res = await apiClient.post('/auth/resend-activation', { email });
    return res.data;
  },

  login: async (payload: LoginPayload): Promise<AuthResponseData> => {
    const res = await apiClient.post('/auth/login', payload);
    return res.data.data;
  },

  refresh: async (): Promise<RefreshedTokenData> => {
    const res = await apiClient.post('/auth/refresh');
    return res.data.data;
  },
};

