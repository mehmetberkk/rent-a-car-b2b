import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';

interface LayoutProps {
  agencySwitcher?: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ agencySwitcher }) => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <Navbar agencySwitcher={agencySwitcher} />
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        © 2026 RentACar B2B Platformu — Filo ve Ortak Araç Havuzu Pazaryeri
      </footer>
    </div>
  );
};

