import React from 'react';
import { PoolListing } from '../../shared/types';

interface PoolVehicleCardProps {
  listing: PoolListing;
  onContactClick: (listing: PoolListing) => void;
  isOwnVehicle?: boolean;
}

export const PoolVehicleCard: React.FC<PoolVehicleCardProps> = ({
  listing,
  onContactClick,
  isOwnVehicle = false,
}) => {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all flex flex-col justify-between overflow-hidden group">
      {/* Card Header & Badges */}
      <div className="p-5">
        <div className="flex items-start justify-between gap-2 mb-3">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">
                {listing.modelYear}
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100">
                📍 {listing.agency.city}
              </span>
              {isOwnVehicle && (
                <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">
                  Kendi Aracınız
                </span>
              )}
            </div>
            <h3 className="text-lg font-extrabold text-slate-900 group-hover:text-indigo-600 transition">
              {listing.brand} {listing.model}
            </h3>
          </div>

          <div className="text-right">
            <span className="text-xs text-slate-400 block font-medium">Günlük Havuz Bedeli</span>
            <div className="text-xl font-black text-emerald-600">
              ₺{Number(listing.dailyPoolPrice).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
            </div>
          </div>
        </div>

        {/* Vehicle Specs Grid */}
        <div className="grid grid-cols-2 gap-2 my-3 text-xs text-slate-600 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400">⛽ Yakıt:</span>
            <span className="font-semibold text-slate-800">{listing.fuelType}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400">⚙️ Vites:</span>
            <span className="font-semibold text-slate-800">{listing.transmission}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400">🏢 Dükkan:</span>
            <span className="font-semibold text-slate-800 truncate" title={listing.agency.name}>
              {listing.agency.name}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400">👤 Yetkili:</span>
            <span className="font-semibold text-slate-800 truncate" title={listing.agency.contactPerson}>
              {listing.agency.contactPerson}
            </span>
          </div>
        </div>
      </div>

      {/* Card Footer Button */}
      <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
        <span className="font-mono text-xs font-bold text-slate-500 bg-white px-2 py-1 rounded border border-slate-200">
          {listing.plate}
        </span>
        <button
          type="button"
          onClick={() => onContactClick(listing)}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-sm transition cursor-pointer"
        >
          <span>İletişime Geç</span>
          <span>→</span>
        </button>
      </div>
    </div>
  );
};

