import React, { useState } from 'react';
import { useAgency } from '../../agency/context/AgencyContext';
import { useVehicles } from '../hooks/useVehicles';
import { Vehicle } from '../../shared/types';
import { VehicleModal } from '../components/VehicleModal';
import { ComplianceBadge } from '../components/ComplianceBadge';
import { StatusSwitcher } from '../components/StatusSwitcher';
import { Link } from 'react-router-dom';

export const FleetListPage: React.FC = () => {
  const { selectedAgency, agencies } = useAgency();
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | null>(null);

  const {
    vehicles,
    isLoading,
    totalElements,
    totalPages,
    createVehicle,
    updateVehicle,
    deleteVehicle,
    updateStatus,
  } = useVehicles(selectedAgency?.id, page);

  if (agencies.length === 0) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 text-center">
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200">
          <div className="text-5xl mb-4">🏢</div>
          <h2 className="text-2xl font-bold text-slate-800 mb-2">Henüz Bir Dükkanınız Bulunmuyor</h2>
          <p className="text-slate-600 mb-6">
            Araç filonuzu oluşturmak ve araç eklemek için öncelikle bir rent a car dükkanı kaydetmelisiniz.
          </p>
          <Link
            to="/agencies"
            className="inline-flex items-center px-6 py-3 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow transition"
          >
            + İlk Dükkanınızı Ekleyin
          </Link>
        </div>
      </div>
    );
  }

  const filteredVehicles = vehicles.filter(
    (v) =>
      v.plate.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.brand.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.model.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleOpenCreate = () => {
    setEditingVehicle(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (v: Vehicle) => {
    setEditingVehicle(v);
    setIsModalOpen(true);
  };

  const handleDelete = async (vehicle: Vehicle) => {
    if (window.confirm(`${vehicle.plate} plakalı aracı silmek istediğinize emin misiniz?`)) {
      try {
        await deleteVehicle(vehicle.id);
      } catch (err: any) {
        alert(err?.response?.data?.detail || 'Araç silinemedi');
      }
    }
  };

  const handleModalSubmit = async (data: any) => {
    if (editingVehicle) {
      await updateVehicle({ id: editingVehicle.id, data });
    } else {
      await createVehicle(data);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold text-slate-900">Filo Yönetimi</h1>
            {selectedAgency && (
              <span className="px-3 py-1 bg-sky-100 text-sky-800 text-xs font-bold rounded-full">
                {selectedAgency.name} ({selectedAgency.city})
              </span>
            )}
          </div>
          <p className="text-sm text-slate-600 mt-1">
            Dükkanınıza kayıtlı araçları izleyin, durumlarını değiştirin ve muayene tarihlerini takip edin.
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-sm transition cursor-pointer"
        >
          <span className="text-lg">+</span> Yeni Araç Ekle
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 mb-6 flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="w-full sm:w-96">
          <input
            type="text"
            placeholder="Plaka, marka veya modele göre ara..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full px-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-sky-500 focus:outline-none"
          />
        </div>
        <div className="text-sm text-slate-500 font-medium">
          Toplam <strong>{totalElements}</strong> araç kayıtlı
        </div>
      </div>

      {/* Vehicles Grid / List */}
      {isLoading ? (
        <div className="py-20 text-center">
          <div className="w-10 h-10 border-4 border-sky-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          <p className="text-slate-500">Filo verileri yükleniyor...</p>
        </div>
      ) : filteredVehicles.length === 0 ? (
        <div className="bg-white p-12 text-center rounded-2xl border border-slate-200 shadow-sm">
          <div className="text-4xl mb-3">🚗</div>
          <h3 className="text-lg font-bold text-slate-800 mb-1">Araç Bulunamadı</h3>
          <p className="text-slate-500 text-sm mb-4">
            {searchTerm ? 'Arama kriterlerinize uyan araç bulunamadı.' : 'Bu dükkana henüz hiç araç eklenmemiş.'}
          </p>
          {!searchTerm && (
            <button
              onClick={handleOpenCreate}
              className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-semibold rounded-lg"
            >
              + Araç Ekle
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filteredVehicles.map((vehicle) => (
            <div
              key={vehicle.id}
              className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col lg:flex-row lg:items-center justify-between gap-4"
            >
              {/* Vehicle Main Info */}
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-1.5 flex-wrap">
                  <span className="font-mono text-base font-bold px-2.5 py-1 bg-slate-900 text-white rounded-md tracking-wider">
                    {vehicle.plate}
                  </span>
                  <h3 className="text-lg font-bold text-slate-900">
                    {vehicle.brand} {vehicle.model}
                  </h3>
                  <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 font-semibold">
                    {vehicle.modelYear}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-slate-50 text-slate-600 border border-slate-200">
                    {vehicle.fuelType}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-slate-50 text-slate-600 border border-slate-200">
                    {vehicle.transmission}
                  </span>
                </div>

                <div className="text-sm font-semibold text-slate-800 mb-3">
                  Günlük Bedel:{' '}
                  <span className="text-emerald-600 text-base font-bold">
                    ₺{Number(vehicle.dailyPoolPrice).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                  </span>
                </div>

                {/* Compliance Badges */}
                <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100">
                  <ComplianceBadge label="Trafik Sigortası" date={vehicle.trafficInsuranceExpiry} />
                  <ComplianceBadge label="Kasko" date={vehicle.cascoExpiry} />
                  <ComplianceBadge label="Araç Muayenesi" date={vehicle.inspectionExpiry} />
                </div>
              </div>

              {/* Status Switcher & Actions */}
              <div className="flex flex-col sm:flex-row lg:flex-col items-start lg:items-end gap-3 justify-between border-t lg:border-t-0 pt-3 lg:pt-0 border-slate-100">
                <div>
                  <span className="text-xs text-slate-500 block mb-1 lg:text-right">Durum Değiştir:</span>
                  <StatusSwitcher
                    vehicleId={vehicle.id}
                    currentStatus={vehicle.status}
                    onStatusChange={(newStatus) => updateStatus({ id: vehicle.id, status: newStatus })}
                  />
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleOpenEdit(vehicle)}
                    className="px-3 py-1.5 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition cursor-pointer"
                  >
                    Düzenle
                  </button>
                  <button
                    onClick={() => handleDelete(vehicle)}
                    className="px-3 py-1.5 text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition cursor-pointer"
                  >
                    Sil
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-2 mt-6">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-semibold text-slate-700 disabled:opacity-40 hover:bg-slate-50 cursor-pointer"
          >
            Önceki
          </button>
          <span className="text-xs text-slate-600 font-medium">
            Sayfa {page + 1} / {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-semibold text-slate-700 disabled:opacity-40 hover:bg-slate-50 cursor-pointer"
          >
            Sonraki
          </button>
        </div>
      )}

      {/* Create / Edit Modal */}
      {selectedAgency && (
        <VehicleModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleModalSubmit}
          vehicle={editingVehicle}
          agencyId={selectedAgency.id}
        />
      )}
    </div>
  );
};
