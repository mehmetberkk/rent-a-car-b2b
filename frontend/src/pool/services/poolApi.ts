import { apiClient } from '../../api/client';
import { PoolListing, PageResult } from '../../shared/types';

export interface PoolFilters {
  city?: string;
  brand?: string;
  fuelType?: string;
  transmission?: string;
  minPrice?: number;
  maxPrice?: number;
  excludeOwn?: boolean;
}

export interface FilterOptions {
  cities: string[];
  brands: string[];
  fuelTypes: string[];
  transmissions: string[];
}

export const poolApi = {
  getListings: async (
    filters: PoolFilters = {},
    page = 0,
    size = 20
  ): Promise<PageResult<PoolListing>> => {
    const params = new URLSearchParams();
    if (filters.city) params.append('city', filters.city);
    if (filters.brand) params.append('brand', filters.brand);
    if (filters.fuelType) params.append('fuelType', filters.fuelType);
    if (filters.transmission) params.append('transmission', filters.transmission);
    if (filters.minPrice !== undefined && filters.minPrice !== null && filters.minPrice > 0) {
      params.append('minPrice', filters.minPrice.toString());
    }
    if (filters.maxPrice !== undefined && filters.maxPrice !== null && filters.maxPrice > 0) {
      params.append('maxPrice', filters.maxPrice.toString());
    }
    if (filters.excludeOwn !== undefined) {
      params.append('excludeOwn', filters.excludeOwn.toString());
    }
    params.append('page', page.toString());
    params.append('size', size.toString());

    const res = await apiClient.get(`/pool/listings?${params.toString()}`);
    return res.data.data;
  },

  getListingByVehicleId: async (vehicleId: string): Promise<PoolListing> => {
    const res = await apiClient.get(`/pool/listings/${vehicleId}`);
    return res.data.data;
  },

  getFilterOptions: async (): Promise<FilterOptions> => {
    const res = await apiClient.get('/pool/filters');
    return res.data.data;
  },
};

