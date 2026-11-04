import React, { useState } from 'react';
import { Vehicle } from '../../shared/types';

interface StatusSwitcherProps {
  vehicleId: string;
  currentStatus: Vehicle['status'];
  onStatusChange: (newStatus: Vehicle['status']) => Promise<any>;
}

export const StatusSwitcher: React.FC<StatusSwitcherProps> = ({
  currentStatus,
  onStatusChange,
}) => {
  const [loadingStatus, setLoadingStatus] = useState<Vehicle['status'] | null>(null);

  const statuses: Array<{
    key: Vehicle['status'];
    label: string;
    activeClass: string;
    inactiveClass: string;
    icon: string;
  }> = [
    {
      key: 'AVAILABLE',
      label: 'Uygun',
      activeClass: 'bg-emerald-600 text-white shadow-sm ring-1 ring-emerald-600',
      inactiveClass: 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100',
      icon: '✓',
    },
    {
      key: 'RENTED',
      label: 'Dolu',
      activeClass: 'bg-slate-700 text-white shadow-sm ring-1 ring-slate-700',
      inactiveClass: 'text-slate-700 bg-slate-100 hover:bg-slate-200',
      icon: '⏳',
    },
    {
      key: 'IN_POOL',
      label: 'Havuzda',
      activeClass: 'bg-indigo-600 text-white shadow-sm ring-1 ring-indigo-600',
      inactiveClass: 'text-indigo-700 bg-indigo-50 hover:bg-indigo-100',
      icon: '🌐',
    },
  ];

  const handleToggle = async (newStatus: Vehicle['status']) => {
    if (newStatus === currentStatus || loadingStatus) return;
    try {
      setLoadingStatus(newStatus);
      await onStatusChange(newStatus);
    } catch (err) {
      console.error('Failed to change status:', err);
    } finally {
      setLoadingStatus(null);
    }
  };

  return (
    <div className="inline-flex rounded-lg p-1 bg-slate-100 border border-slate-200 gap-1 text-xs font-medium">
      {statuses.map((s) => {
        const isActive = currentStatus === s.key;
        const isLoading = loadingStatus === s.key;

        return (
          <button
            key={s.key}
            type="button"
            disabled={loadingStatus !== null}
            onClick={() => handleToggle(s.key)}
            className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 cursor-pointer disabled:cursor-not-allowed ${
              isActive ? s.activeClass : s.inactiveClass
            }`}
            title={`Durumu "${s.label}" olarak değiştir`}
          >
            {isLoading ? (
              <span className="w-3 h-3 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
            ) : (
              <span>{s.icon}</span>
            )}
            <span>{s.label}</span>
          </button>
        );
      })}
    </div>
  );
};
