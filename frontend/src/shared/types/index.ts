export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  status: 'PENDING' | 'ACTIVE';
}

export interface Agency {
  id: string;
  ownerId?: string;
  name: string;
  city: string;
  district?: string | null;
  phone: string;
  contactPerson: string;
  address: string;
  createdAt?: string;
}

export interface Vehicle {
  id: string;
  agencyId: string;
  plate: string;
  brand: string;
  model: string;
  modelYear: number;
  fuelType: 'GASOLINE' | 'DIESEL' | 'ELECTRIC' | 'HYBRID';
  transmission: 'MANUAL' | 'AUTOMATIC';
  dailyPoolPrice: number;
  status: 'AVAILABLE' | 'RENTED' | 'IN_POOL';
  trafficInsuranceExpiry?: string | null;
  cascoExpiry?: string | null;
  inspectionExpiry?: string | null;
  createdAt?: string;
}

export interface PoolListing {
  id?: string;
  vehicleId: string;
  agencyId: string;
  plate: string;
  brand: string;
  model: string;
  modelYear: number;
  fuelType: string;
  transmission: string;
  dailyPoolPrice: number;
  listedAt: string;
  agency: {
    id: string;
    name: string;
    city: string;
    phone: string;
    contactPerson: string;
    address: string;
  };
}

export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

