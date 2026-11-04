import { apiClient } from '../../api/client';
import { Vehicle, PageResult } from '../../shared/types';

export interface CreateVehicleData {
  agencyId: string;
  plate: string;
  brand: string;
  model: string;
  modelYear: number;
  fuelType: 'GASOLINE' | 'DIESEL' | 'ELECTRIC' | 'HYBRID';
  transmission: 'MANUAL' | 'AUTOMATIC';
  dailyPoolPrice: number;
  trafficInsuranceExpiry?: string | null;
  cascoExpiry?: string | null;
  inspectionExpiry?: string | null;
}

export interface UpdateVehicleData {
  plate: string;
  brand: string;
  model: string;
  modelYear: number;
  fuelType: 'GASOLINE' | 'DIESEL' | 'ELECTRIC' | 'HYBRID';
  transmission: 'MANUAL' | 'AUTOMATIC';
  dailyPoolPrice: number;
  trafficInsuranceExpiry?: string | null;
  cascoExpiry?: string | null;
  inspectionExpiry?: string | null;
}

export const fleetApi = {
  getVehicles: async (agencyId: string, page = 0, size = 20): Promise<PageResult<Vehicle>> => {
    const res = await apiClient.get(`/vehicles?agencyId=${agencyId}&page=${page}&size=${size}`);
    return res.data.data;
  },

  getVehicleById: async (id: string): Promise<Vehicle> => {
    const res = await apiClient.get(`/vehicles/${id}`);
    return res.data.data;
  },

  createVehicle: async (data: CreateVehicleData): Promise<Vehicle> => {
    const res = await apiClient.post('/vehicles', data);
    return res.data.data;
  },

  updateVehicle: async (id: string, data: UpdateVehicleData): Promise<Vehicle> => {
    const res = await apiClient.put(`/vehicles/${id}`, data);
    return res.data.data;
  },

  deleteVehicle: async (id: string): Promise<void> => {
    await apiClient.delete(`/vehicles/${id}`);
  },

  updateVehicleStatus: async (
    id: string,
    status: 'AVAILABLE' | 'RENTED' | 'IN_POOL'
  ): Promise<{ id: string; status: 'AVAILABLE' | 'RENTED' | 'IN_POOL'; previousStatus: string }> => {
    const res = await apiClient.patch(`/vehicles/${id}/status`, { status });
    return res.data.data;
  },
};
