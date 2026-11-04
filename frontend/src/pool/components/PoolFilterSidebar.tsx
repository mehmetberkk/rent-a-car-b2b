import React from 'react';
import { FilterOptions, PoolFilters } from '../services/poolApi';

interface PoolFilterSidebarProps {
  filters: PoolFilters;
  filterOptions: FilterOptions;
  onChange: (newFilters: PoolFilters) => void;
  onReset: () => void;
}

export const PoolFilterSidebar: React.FC<PoolFilterSidebarProps> = ({
  filters,
  filterOptions,
  onChange,
  onReset,
}) => {
  const handleChange = (field: keyof PoolFilters, value: any) => {
    onChange({
      ...filters,
      [field]: value,
    });
  };

  const hasActiveFilters = Boolean(
    filters.city ||
      filters.brand ||
      filters.fuelType ||
      filters.transmission ||
      filters.minPrice ||
      filters.maxPrice ||
      filters.excludeOwn === false
  );

  return (
    <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-5">
      <div className="flex items-center justify-between pb-3 border-b border-slate-100">
        <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
          <span>🔍</span> Filtreler
        </h3>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={onReset}
            className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold cursor-pointer"
          >
            Sıfırla
          </button>
        )}
      </div>

      {/* Exclude own vehicles switch */}
      <div className="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-200">
        <label htmlFor="excludeOwn" className="text-xs font-semibold text-slate-700 cursor-pointer">
          Kendi Araçlarımı Gizle
        </label>
        <input
          id="excludeOwn"
          type="checkbox"
          checked={filters.excludeOwn ?? true}
          onChange={(e) => handleChange('excludeOwn', e.target.checked)}
          className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500 cursor-pointer"
        />
      </div>

      {/* City */}
      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1.5">Şehir</label>
        <select
          value={filters.city || ''}
          onChange={(e) => handleChange('city', e.target.value || undefined)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        >
          <option value="">Tüm Şehirler</option>
          {filterOptions.cities.map((city) => (
            <option key={city} value={city}>
              {city}
            </option>
          ))}
        </select>
      </div>

      {/* Brand */}
      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1.5">Marka</label>
        <select
          value={filters.brand || ''}
          onChange={(e) => handleChange('brand', e.target.value || undefined)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        >
          <option value="">Tüm Markalar</option>
          {filterOptions.brands.map((brand) => (
            <option key={brand} value={brand}>
              {brand}
            </option>
          ))}
        </select>
      </div>

      {/* Fuel Type */}
      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1.5">Yakıt Tipi</label>
        <select
          value={filters.fuelType || ''}
          onChange={(e) => handleChange('fuelType', e.target.value || undefined)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        >
          <option value="">Tümü</option>
          {filterOptions.fuelTypes.map((fuel) => (
            <option key={fuel} value={fuel}>
              {fuel}
            </option>
          ))}
        </select>
      </div>

      {/* Transmission */}
      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1.5">Vites Tipi</label>
        <select
          value={filters.transmission || ''}
          onChange={(e) => handleChange('transmission', e.target.value || undefined)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        >
          <option value="">Tümü</option>
          {filterOptions.transmissions.map((trans) => (
            <option key={trans} value={trans}>
              {trans}
            </option>
          ))}
        </select>
      </div>

      {/* Price Range */}
      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1.5">
          Günlük Fiyat Aralığı (TL)
        </label>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            min={0}
            placeholder="Min"
            value={filters.minPrice || ''}
            onChange={(e) => handleChange('minPrice', e.target.value ? Number(e.target.value) : undefined)}
            className="w-full px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
          />
          <input
            type="number"
            min={0}
            placeholder="Max"
            value={filters.maxPrice || ''}
            onChange={(e) => handleChange('maxPrice', e.target.value ? Number(e.target.value) : undefined)}
            className="w-full px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>
    </div>
  );
};

