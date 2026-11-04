import axios, { AxiosError } from 'axios';

export interface InvalidParam {
  name: string;
  reason: string;
  rejectedValue?: unknown;
}

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  invalidParams?: InvalidParam[];
}

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetails>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (!window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register') && !window.location.pathname.startsWith('/activate')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

