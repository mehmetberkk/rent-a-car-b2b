import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/context/AuthContext';
import { AgencyProvider } from './agency/context/AgencyContext';
import { ProtectedRoute } from './shared/components/ProtectedRoute';
import { Layout } from './shared/components/Layout';
import { LoginPage } from './auth/pages/LoginPage';
import { RegisterPage } from './auth/pages/RegisterPage';
import { ActivatePage } from './auth/pages/ActivatePage';
import { AgencyListPage } from './agency/pages/AgencyListPage';
import { AgencySwitcher } from './agency/components/AgencySwitcher';
import { FleetListPage } from './fleet/pages/FleetListPage';
import { PoolMarketplacePage } from './pool/pages/PoolMarketplacePage';

const App: React.FC = () => {
  return (
    <AuthProvider>
      <AgencyProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/activate" element={<ActivatePage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Layout agencySwitcher={<AgencySwitcher />} />}>
              <Route index element={<Navigate to="/fleet" replace />} />
              <Route path="fleet" element={<FleetListPage />} />
              <Route path="pool" element={<PoolMarketplacePage />} />
              <Route path="agencies" element={<AgencyListPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AgencyProvider>
    </AuthProvider>
  );
};

export default App;
