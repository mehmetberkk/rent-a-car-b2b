import React, { useState, useRef, useEffect } from 'react';
import { useAgency } from '../context/AgencyContext';
import { Building2, ChevronDown, Plus, Check } from 'lucide-react';
import { Link } from 'react-router-dom';

export const AgencySwitcher: React.FC = () => {
  const { agencies, selectedAgency, selectAgency } = useAgency();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (agencies.length === 0) {
    return (
      <Link
        to="/agencies"
        className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-50 text-amber-800 border border-amber-200 rounded-lg text-xs font-semibold hover:bg-amber-100 transition-colors"
      >
        <Plus className="w-3.5 h-3.5" />
        Dükkan Ekle
      </Link>
    );
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-1.5 bg-slate-100 hover:bg-slate-200/80 rounded-xl text-xs font-medium text-slate-800 transition-all border border-slate-200/60"
      >
        <Building2 className="w-3.5 h-3.5 text-brand-600" />
        <span className="font-semibold max-w-[140px] truncate">
          {selectedAgency ? selectedAgency.name : 'Dükkan Seçiniz'}
        </span>
        {selectedAgency && (
          <span className="text-[11px] text-slate-500 font-normal">({selectedAgency.city})</span>
        )}
        <ChevronDown className="w-3.5 h-3.5 text-slate-400 ml-1" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-white rounded-xl shadow-lg border border-slate-200 py-1.5 z-50">
          <div className="px-3 py-2 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            Dükkanlarınız
          </div>
          <div className="max-h-60 overflow-y-auto">
            {agencies.map((agency) => (
              <button
                key={agency.id}
                onClick={() => {
                  selectAgency(agency.id);
                  setIsOpen(false);
                }}
                className={`w-full text-left px-3 py-2 text-xs flex items-center justify-between hover:bg-slate-50 transition-colors ${
                  selectedAgency?.id === agency.id ? 'bg-brand-50/60 text-brand-700 font-semibold' : 'text-slate-700'
                }`}
              >
                <div className="truncate mr-2">
                  <div className="truncate">{agency.name}</div>
                  <div className="text-[10px] text-slate-400">{agency.city}{agency.district ? ` / ${agency.district}` : ''}</div>
                </div>
                {selectedAgency?.id === agency.id && (
                  <Check className="w-3.5 h-3.5 text-brand-600 shrink-0" />
                )}
              </button>
            ))}
          </div>

          <div className="border-t border-slate-100 mt-1 pt-1 px-2">
            <Link
              to="/agencies"
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2 w-full px-2 py-1.5 text-xs text-brand-600 hover:bg-brand-50 rounded-lg font-medium transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Yeni Dükkan Ekle / Yönet
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};

