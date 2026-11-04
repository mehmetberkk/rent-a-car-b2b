import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fleetApi, CreateVehicleData, UpdateVehicleData } from '../services/fleetApi';

export const useVehicles = (agencyId?: string, page = 0, size = 20) => {
  const queryClient = useQueryClient();

  const vehiclesQuery = useQuery({
    queryKey: ['vehicles', agencyId, page, size],
    queryFn: () => (agencyId ? fleetApi.getVehicles(agencyId, page, size) : Promise.resolve(null)),
    enabled: Boolean(agencyId),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateVehicleData) => fleetApi.createVehicle(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateVehicleData }) =>
      fleetApi.updateVehicle(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => fleetApi.deleteVehicle(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({
      id,
      status,
    }: {
      id: string;
      status: 'AVAILABLE' | 'RENTED' | 'IN_POOL';
    }) => fleetApi.updateVehicleStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
      queryClient.invalidateQueries({ queryKey: ['pool-listings'] });
    },
  });

  return {
    ...vehiclesQuery,
    vehicles: vehiclesQuery.data?.content || [],
    totalElements: vehiclesQuery.data?.totalElements || 0,
    totalPages: vehiclesQuery.data?.totalPages || 0,
    createVehicle: createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    updateVehicle: updateMutation.mutateAsync,
    isUpdating: updateMutation.isPending,
    deleteVehicle: deleteMutation.mutateAsync,
    isDeleting: deleteMutation.isPending,
    updateStatus: updateStatusMutation.mutateAsync,
    isUpdatingStatus: updateStatusMutation.isPending,
  };
};

