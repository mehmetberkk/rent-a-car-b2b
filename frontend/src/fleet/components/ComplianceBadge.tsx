import React from 'react';

interface ComplianceBadgeProps {
  label: string;
  date?: string | null;
}

export const ComplianceBadge: React.FC<ComplianceBadgeProps> = ({ label, date }) => {
  if (!date) {
    return (
      <div className="flex items-center text-xs text-slate-400">
        <span className="font-medium mr-1.5">{label}:</span>
        <span>Girilmedi</span>
      </div>
    );
  }

  const expiry = new Date(date);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  expiry.setHours(0, 0, 0, 0);

  const diffTime = expiry.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  if (diffDays < 0) {
    return (
      <div className="flex items-center text-xs font-semibold px-2 py-0.5 rounded-full bg-red-100 text-red-800 border border-red-200">
        <span className="w-1.5 h-1.5 rounded-full bg-red-600 mr-1.5 animate-pulse"></span>
        <span>{label}: Süresi Geçti ({Math.abs(diffDays)} gün)</span>
      </div>
    );
  }

  if (diffDays <= 30) {
    return (
      <div className="flex items-center text-xs font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">
        <span className="w-1.5 h-1.5 rounded-full bg-amber-600 mr-1.5"></span>
        <span>{label}: Yaklaşıyor ({diffDays} gün kaldı)</span>
      </div>
    );
  }

  return (
    <div className="flex items-center text-xs text-slate-600 px-2 py-0.5 rounded-full bg-slate-100 border border-slate-200">
      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5"></span>
      <span>{label}: {date} ({diffDays} gün)</span>
    </div>
  );
};

