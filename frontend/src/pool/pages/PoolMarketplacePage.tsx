import React, { useState } from 'react';
import { usePoolListings } from '../hooks/usePoolListings';
import { PoolFilters } from '../services/poolApi';
import { PoolVehicleCard } from '../components/PoolVehicleCard';
import { PoolFilterSidebar } from '../components/PoolFilterSidebar';
import { AgencyContactModal } from '../components/AgencyContactModal';
import { PoolListing } from '../../shared/types';
import { useAuth } from '../../auth/context/AuthContext';

export const PoolMarketplacePage: React.FC = () => {
  const { agencyIds } = useAuth();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<PoolFilters>({
    excludeOwn: true,
  });
  const [selectedListing, setSelectedListing] = useState<PoolListing | null>(null);
  const [isContactModalOpen, setIsContactModalOpen] = useState(false);

  const {
    listings,
    isLoading,
    totalElements,
    totalPages,
    filterOptions,
    refetch,
  } = usePoolListings(filters, page, 12);

  const handleFilterChange = (newFilters: PoolFilters) => {
    setFilters(newFilters);
    setPage(0);
  };

  const handleResetFilters = () => {
    setFilters({ excludeOwn: true });
    setPage(0);
  };

  const handleContactClick = (listing: PoolListing) => {
    setSelectedListing(listing);
    setIsContactModalOpen(true);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-slate-900">Ortak Araç Havuzu</h1>
            <span className="px-3 py-1 bg-indigo-100 text-indigo-800 text-xs font-bold rounded-full">
              B2B Marketplace
            </span>
          </div>
          <p className="text-sm text-slate-600 mt-1">
            Sisteme kayıtlı rent a car dükkanlarının ortak paylaşıma açtığı müsait araçları inceleyin ve doğrudan iletişime geçin.
          </p>
        </div>

        <button
          onClick={() => refetch()}
          type="button"
          className="inline-flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 transition cursor-pointer"
        >
          <span>🔄</span> Havuzu Yenile
        </button>
      </div>

      {/* Main Content Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
        {/* Filters Sidebar */}
        <div className="lg:col-span-1">
          <PoolFilterSidebar
            filters={filters}
            filterOptions={filterOptions}
            onChange={handleFilterChange}
            onReset={handleResetFilters}
          />
        </div>

        {/* Listings Content */}
        <div className="lg:col-span-3">
          <div className="mb-4 flex items-center justify-between">
            <span className="text-sm text-slate-600 font-medium">
              Ortak havuzda <strong>{totalElements}</strong> araç listeleniyor
            </span>
          </div>

          {isLoading ? (
            <div className="py-24 text-center">
              <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
              <p className="text-slate-500 text-sm">Havuzdaki araçlar yükleniyor...</p>
            </div>
          ) : listings.length === 0 ? (
            <div className="bg-white p-16 text-center rounded-2xl border border-slate-200 shadow-sm">
              <div className="text-5xl mb-4">🌐</div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">Havuzda Araç Bulunamadı</h3>
              <p className="text-slate-500 text-sm max-w-md mx-auto mb-6">
                Belirlediğiniz filtre kriterlerine uygun havuz aracı bulunmuyor. Filtreleri esneterek veya sıfırlayarak tekrar arayabilirsiniz.
              </p>
              <button
                onClick={handleResetFilters}
                className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow transition cursor-pointer"
              >
                Filtreleri Sıfırla
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {listings.map((listing) => (
                <PoolVehicleCard
                  key={listing.id || listing.vehicleId}
                  listing={listing}
                  onContactClick={handleContactClick}
                  isOwnVehicle={agencyIds.includes(listing.agencyId)}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-8">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 disabled:opacity-40 hover:bg-white bg-slate-50 cursor-pointer"
              >
                Önceki
              </button>
              <span className="text-xs text-slate-600 font-medium px-2">
                Sayfa {page + 1} / {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 disabled:opacity-40 hover:bg-white bg-slate-50 cursor-pointer"
              >
                Sonraki
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Agency Contact Modal */}
      <AgencyContactModal
        isOpen={isContactModalOpen}
        onClose={() => setIsContactModalOpen(false)}
        listing={selectedListing}
      />
    </div>
  );
};

