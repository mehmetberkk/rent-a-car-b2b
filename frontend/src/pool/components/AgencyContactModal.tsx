import React, { useState } from 'react';
import { PoolListing } from '../../shared/types';

interface AgencyContactModalProps {
  isOpen: boolean;
  onClose: () => void;
  listing: PoolListing | null;
}

export const AgencyContactModal: React.FC<AgencyContactModalProps> = ({
  isOpen,
  onClose,
  listing,
}) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen || !listing) return null;

  const { agency } = listing;

  const handleCopyPhone = () => {
    if (agency.phone) {
      navigator.clipboard.writeText(agency.phone);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 animate-in fade-in zoom-in duration-150">
        <div className="flex justify-between items-center pb-4 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <span className="text-2xl">🏢</span>
            <h3 className="text-xl font-bold text-slate-800">Dükkan İletişim Kartı</h3>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 rounded-lg p-1 text-lg cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Selected Vehicle Summary */}
        <div className="mt-4 p-3 bg-slate-50 rounded-xl border border-slate-200">
          <span className="text-xs text-slate-500 font-semibold uppercase block mb-1">
            İlgilenilen Havuz Aracı
          </span>
          <div className="flex items-center justify-between">
            <div>
              <span className="font-bold text-slate-900">
                {listing.brand} {listing.model}
              </span>
              <span className="text-xs text-slate-500 ml-2">({listing.modelYear})</span>
            </div>
            <div className="text-emerald-700 font-bold text-sm">
              ₺{Number(listing.dailyPoolPrice).toLocaleString('tr-TR', { minimumFractionDigits: 2 })} / gün
            </div>
          </div>
          <div className="mt-1 font-mono text-xs font-semibold text-slate-700">
            Plaka: {listing.plate}
          </div>
        </div>

        {/* Agency Details */}
        <div className="mt-5 space-y-4">
          <div>
            <label className="text-xs font-semibold text-slate-400 uppercase">Dükkan / Şube Adı</label>
            <p className="text-base font-bold text-slate-900">{agency.name}</p>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 uppercase">Yetkili / Sorumlu Kişi</label>
            <p className="text-sm font-semibold text-slate-800">{agency.contactPerson}</p>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 uppercase">Telefon Numarası</label>
            <div className="flex items-center gap-2 mt-1">
              <a
                href={`tel:${agency.phone}`}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded-lg text-sm font-bold transition border border-emerald-200"
              >
                <span>📞</span>
                <span>{agency.phone}</span>
              </a>
              <button
                onClick={handleCopyPhone}
                type="button"
                className="px-2.5 py-1.5 text-xs text-slate-600 hover:text-slate-900 border border-slate-200 rounded-lg hover:bg-slate-50 cursor-pointer"
              >
                {copied ? 'Kopyalandı! ✓' : 'Kopyala'}
              </button>
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 uppercase">Şehir & Adres</label>
            <p className="text-sm text-slate-800 font-medium">📍 {agency.city}</p>
            <p className="text-xs text-slate-600 mt-0.5 leading-relaxed bg-slate-50 p-2.5 rounded-lg border border-slate-100">
              {agency.address}
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="mt-6 pt-4 border-t border-slate-100 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="w-full sm:w-auto px-6 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-sm font-semibold cursor-pointer shadow-sm transition"
          >
            Kapat
          </button>
        </div>
      </div>
    </div>
  );
};

