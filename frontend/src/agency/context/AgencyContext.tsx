import React, { createContext, useContext, useState, useEffect } from 'react';
import { Agency } from '../../shared/types';
import { agencyApi, CreateAgencyPayload } from '../services/agencyApi';
import { useAuth } from '../../auth/context/AuthContext';

interface AgencyContextType {
  agencies: Agency[];
  selectedAgency: Agency | null;
  loading: boolean;
  selectAgency: (agencyId: string | null) => void;
  createAgency: (payload: CreateAgencyPayload) => Promise<Agency>;
  updateAgency: (id: string, payload: CreateAgencyPayload) => Promise<void>;
  deleteAgency: (id: string) => Promise<void>;
  refreshAgencies: () => Promise<void>;
}

const AgencyContext = createContext<AgencyContextType | undefined>(undefined);

export const AgencyProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, updateToken } = useAuth();
  const [agencies, setAgencies] = useState<Agency[]>([]);
  const [selectedAgency, setSelectedAgency] = useState<Agency | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchAgencies = async () => {
    if (!isAuthenticated) {
      setAgencies([]);
      setSelectedAgency(null);
      return;
    }

    setLoading(true);
    try {
      const data = await agencyApi.getAgencies();
      setAgencies(data);

      const savedAgencyId = localStorage.getItem('selectedAgencyId');
      if (savedAgencyId && data.some((a) => a.id === savedAgencyId)) {
        setSelectedAgency(data.find((a) => a.id === savedAgencyId) || null);
      } else if (data.length > 0) {
        setSelectedAgency(data[0]);
        localStorage.setItem('selectedAgencyId', data[0].id);
      } else {
        setSelectedAgency(null);
        localStorage.removeItem('selectedAgencyId');
      }
    } catch (err) {
      console.error('Dükkanlar yüklenirken hata:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAgencies();
  }, [isAuthenticated]);

  const selectAgency = (agencyId: string | null) => {
    if (!agencyId) {
      setSelectedAgency(null);
      localStorage.removeItem('selectedAgencyId');
      return;
    }
    const found = agencies.find((a) => a.id === agencyId) || null;
    setSelectedAgency(found);
    if (found) {
      localStorage.setItem('selectedAgencyId', found.id);
    }
  };

  const createAgency = async (payload: CreateAgencyPayload): Promise<Agency> => {
    const res = await agencyApi.createAgency(payload);
    // Immediately update token with new agencyId claim so downstream fleet requests succeed!
    if (res.refreshedToken) {
      updateToken(res.refreshedToken.accessToken, res.refreshedToken.agencyIds);
    }

    setAgencies((prev) => [...prev, res.agency]);
    setSelectedAgency(res.agency);
    localStorage.setItem('selectedAgencyId', res.agency.id);
    return res.agency;
  };

  const updateAgency = async (id: string, payload: CreateAgencyPayload) => {
    const updated = await agencyApi.updateAgency(id, payload);
    setAgencies((prev) => prev.map((a) => (a.id === id ? updated : a)));
    if (selectedAgency?.id === id) {
      setSelectedAgency(updated);
    }
  };

  const deleteAgency = async (id: string) => {
    const res = await agencyApi.deleteAgency(id);
    if (res.refreshedToken) {
      updateToken(res.refreshedToken.accessToken, res.refreshedToken.agencyIds);
    }

    const remaining = agencies.filter((a) => a.id !== id);
    setAgencies(remaining);
    if (selectedAgency?.id === id) {
      const nextAgency = remaining.length > 0 ? remaining[0] : null;
      setSelectedAgency(nextAgency);
      if (nextAgency) {
        localStorage.setItem('selectedAgencyId', nextAgency.id);
      } else {
        localStorage.removeItem('selectedAgencyId');
      }
    }
  };

  return (
    <AgencyContext.Provider
      value={{
        agencies,
        selectedAgency,
        loading,
        selectAgency,
        createAgency,
        updateAgency,
        deleteAgency,
        refreshAgencies: fetchAgencies,
      }}
    >
      {children}
    </AgencyContext.Provider>
  );
};

export const useAgency = () => {
  const context = useContext(AgencyContext);
  if (!context) {
    throw new Error('useAgency must be used within an AgencyProvider');
  }
  return context;
};

