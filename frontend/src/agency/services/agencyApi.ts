import { apiClient } from '../../api/client';
import { Agency } from '../../shared/types';
import { RefreshedTokenData } from '../../auth/services/authApi';

export interface CreateAgencyPayload {
  name: string;
  city: string;
  district?: string;
  phone: string;
  contactPerson: string;
  address: string;
}

export interface CreateAgencyResult {
  agency: Agency;
  refreshedToken: RefreshedTokenData;
}

export interface DeleteAgencyResult {
  message: string;
  refreshedToken: RefreshedTokenData;
}

export const agencyApi = {
  getAgencies: async (): Promise<Agency[]> => {
    const res = await apiClient.get('/agencies');
    return res.data.data;
  },

  getAgencyById: async (id: string): Promise<Agency> => {
    const res = await apiClient.get(`/agencies/${id}`);
    return res.data.data;
  },

  createAgency: async (payload: CreateAgencyPayload): Promise<CreateAgencyResult> => {
    const res = await apiClient.post('/agencies', payload);
    return res.data.data;
  },

  updateAgency: async (id: string, payload: CreateAgencyPayload): Promise<Agency> => {
    const res = await apiClient.put(`/agencies/${id}`, payload);
    return res.data.data;
  },

  deleteAgency: async (id: string): Promise<DeleteAgencyResult> => {
    const res = await apiClient.delete(`/agencies/${id}`);
    return res.data.data;
  },
};

