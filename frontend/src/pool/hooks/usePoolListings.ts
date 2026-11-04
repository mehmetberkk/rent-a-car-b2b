import { useQuery } from '@tanstack/react-query';
import { poolApi, PoolFilters } from '../services/poolApi';

export const usePoolListings = (filters: PoolFilters = {}, page = 0, size = 20) => {
  const listingsQuery = useQuery({
    queryKey: ['pool-listings', filters, page, size],
    queryFn: () => poolApi.getListings(filters, page, size),
  });

  const filterOptionsQuery = useQuery({
    queryKey: ['pool-filters'],
    queryFn: () => poolApi.getFilterOptions(),
    staleTime: 5 * 60 * 1000, // 5 min cache
  });

  return {
    ...listingsQuery,
    listings: listingsQuery.data?.content || [],
    totalElements: listingsQuery.data?.totalElements || 0,
    totalPages: listingsQuery.data?.totalPages || 0,
    filterOptions: filterOptionsQuery.data || {
      cities: [],
      brands: [],
      fuelTypes: [],
      transmissions: [],
    },
    isLoadingFilters: filterOptionsQuery.isLoading,
  };
};

