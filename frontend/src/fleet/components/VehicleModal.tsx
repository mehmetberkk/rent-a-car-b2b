import React, { useState, useEffect } from 'react';
import { Vehicle } from '../../shared/types';
import { CreateVehicleData, UpdateVehicleData } from '../services/fleetApi';

interface VehicleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateVehicleData | UpdateVehicleData) => Promise<any>;
  vehicle?: Vehicle | null;
  agencyId: string;
}

export const VehicleModal: React.FC<VehicleModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  vehicle,
  agencyId,
}) => {
  const [formData, setFormData] = useState<CreateVehicleData>({
    agencyId,
    plate: '',
    brand: '',
    model: '',
    modelYear: new Date().getFullYear(),
    fuelType: 'GASOLINE',
    transmission: 'MANUAL',
    dailyPoolPrice: 1000,
    trafficInsuranceExpiry: '',
    cascoExpiry: '',
    inspectionExpiry: '',
  });

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (vehicle) {
      setFormData({
        agencyId: vehicle.agencyId,
        plate: vehicle.plate,
        brand: vehicle.brand,
        model: vehicle.model,
        modelYear: vehicle.modelYear,
        fuelType: vehicle.fuelType,
        transmission: vehicle.transmission,
        dailyPoolPrice: vehicle.dailyPoolPrice,
        trafficInsuranceExpiry: vehicle.trafficInsuranceExpiry || '',
        cascoExpiry: vehicle.cascoExpiry || '',
        inspectionExpiry: vehicle.inspectionExpiry || '',
      });
    } else {
      setFormData({
        agencyId,
        plate: '',
        brand: '',
        model: '',
        modelYear: new Date().getFullYear(),
        fuelType: 'GASOLINE',
        transmission: 'MANUAL',
        dailyPoolPrice: 1000,
        trafficInsuranceExpiry: '',
        cascoExpiry: '',
        inspectionExpiry: '',
      });
    }
    setError(null);
  }, [vehicle, agencyId, isOpen]);

  if (!isOpen) return null;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? Number(value) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const payload = {
        ...formData,
        plate: formData.plate.toUpperCase().replace(/\s+/g, ''),
        trafficInsuranceExpiry: formData.trafficInsuranceExpiry || null,
        cascoExpiry: formData.cascoExpiry || null,
        inspectionExpiry: formData.inspectionExpiry || null,
      };

      await onSubmit(payload);
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.detail || err?.message || 'Bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full p-6 animate-in fade-in zoom-in duration-150">
        <div className="flex justify-between items-center pb-4 border-b border-slate-100">
          <h3 className="text-xl font-bold text-slate-800">
            {vehicle ? 'Araç Bilgilerini Düzenle' : 'Yeni Araç Ekle'}
          </h3>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 rounded-lg p-1"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Plaka *
              </label>
              <input
                type="text"
                name="plate"
                required
                placeholder="34ABC123"
                value={formData.plate}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500 font-mono uppercase"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Marka *
              </label>
              <input
                type="text"
                name="brand"
                required
                placeholder="Renault, Fiat, Ford..."
                value={formData.brand}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Model *
              </label>
              <input
                type="text"
                name="model"
                required
                placeholder="Clio, Egea, Focus..."
                value={formData.model}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Model Yılı *
              </label>
              <input
                type="number"
                name="modelYear"
                required
                min={1980}
                max={new Date().getFullYear() + 1}
                value={formData.modelYear}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Yakıt Tipi *
              </label>
              <select
                name="fuelType"
                value={formData.fuelType}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
              >
                <option value="GASOLINE">Benzin</option>
                <option value="DIESEL">Dizel</option>
                <option value="HYBRID">Hibrit</option>
                <option value="ELECTRIC">Elektrik</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Vites Tipi *
              </label>
              <select
                name="transmission"
                value={formData.transmission}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
              >
                <option value="MANUAL">Manuel</option>
                <option value="AUTOMATIC">Otomatik</option>
              </select>
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Günlük Havuz / Kira Bedeli (TL) *
              </label>
              <input
                type="number"
                name="dailyPoolPrice"
                required
                min={1}
                step="0.01"
                placeholder="1500.00"
                value={formData.dailyPoolPrice}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
              />
            </div>
          </div>

          <div className="border-t border-slate-200 pt-3">
            <h4 className="text-sm font-semibold text-slate-700 mb-2">
              Operasyonel Muayene & Sigorta Bilgileri
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs text-slate-600 mb-1">Trafik Sigortası Bitiş</label>
                <input
                  type="date"
                  name="trafficInsuranceExpiry"
                  value={formData.trafficInsuranceExpiry || ''}
                  onChange={handleChange}
                  className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-600 mb-1">Kasko Bitiş</label>
                <input
                  type="date"
                  name="cascoExpiry"
                  value={formData.cascoExpiry || ''}
                  onChange={handleChange}
                  className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-600 mb-1">Araç Muayenesi Bitiş</label>
                <input
                  type="date"
                  name="inspectionExpiry"
                  value={formData.inspectionExpiry || ''}
                  onChange={handleChange}
                  className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500"
                />
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-50 cursor-pointer"
            >
              İptal
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-lg text-sm font-semibold cursor-pointer disabled:opacity-50"
            >
              {loading ? 'Kaydediliyor...' : vehicle ? 'Güncelle' : 'Araç Ekle'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

