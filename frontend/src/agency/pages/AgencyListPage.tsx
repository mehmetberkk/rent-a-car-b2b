import React, { useState } from 'react';
import { useAgency } from '../context/AgencyContext';
import { Building2, Plus, Phone, MapPin, User as UserIcon, Edit2, Trash2, Check, AlertCircle } from 'lucide-react';
import { CreateAgencyPayload } from '../services/agencyApi';

export const AgencyListPage: React.FC = () => {
  const { agencies, selectedAgency, selectAgency, createAgency, updateAgency, deleteAgency, loading } = useAgency();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Form states
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [district, setDistrict] = useState('');
  const [phone, setPhone] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [address, setAddress] = useState('');

  const openCreateModal = () => {
    setEditingId(null);
    setName('');
    setCity('');
    setDistrict('');
    setPhone('');
    setContactPerson('');
    setAddress('');
    setErrorMessage(null);
    setModalOpen(true);
  };

  const openEditModal = (agency: any) => {
    setEditingId(agency.id);
    setName(agency.name);
    setCity(agency.city);
    setDistrict(agency.district || '');
    setPhone(agency.phone);
    setContactPerson(agency.contactPerson);
    setAddress(agency.address);
    setErrorMessage(null);
    setModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSubmitting(true);

    const payload: CreateAgencyPayload = {
      name,
      city,
      district: district || undefined,
      phone,
      contactPerson,
      address,
    };

    try {
      if (editingId) {
        await updateAgency(editingId, payload);
      } else {
        await createAgency(payload);
      }
      setModalOpen(false);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.detail || 'İşlem sırasında bir hata oluştu.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string, agencyName: string) => {
    if (window.confirm(`"${agencyName}" dükkanını silmek istediğinize emin misiniz?`)) {
      try {
        await deleteAgency(id);
      } catch (err: any) {
        alert(err.response?.data?.detail || 'Dükkan silinemedi. Bağlı araçlar bulunuyor olabilir.');
      }
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Rent a Car Dükkanlarım (Şubeler)</h1>
          <p className="text-sm text-slate-500 mt-1">
            Filonuzu ve ortak havuzdaki araçlarınızı şube bazında yönetebilir, dükkan iletişim bilgilerinizi güncelleyebilirsiniz.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-2 px-4 py-2.5 bg-brand-600 hover:bg-brand-700 text-white rounded-xl text-sm font-semibold transition-all shadow-sm shrink-0"
        >
          <Plus className="w-4 h-4" />
          Yeni Dükkan Ekle
        </button>
      </div>

      {loading && agencies.length === 0 ? (
        <div className="py-12 text-center text-slate-500 text-sm">Dükkanlar yükleniyor...</div>
      ) : agencies.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300 p-8">
          <Building2 className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">Henüz bir dükkanınız bulunmuyor</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto mb-6">
            Araç ekleyebilmek ve havuzda işlem yapabilmek için ilk şubenizi ekleyerek başlayın.
          </p>
          <button
            onClick={openCreateModal}
            className="px-4 py-2 bg-brand-600 text-white rounded-xl text-sm font-medium hover:bg-brand-700 transition-colors"
          >
            İlk Dükkanınızı Ekleyin
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {agencies.map((agency) => {
            const isSelected = selectedAgency?.id === agency.id;
            return (
              <div
                key={agency.id}
                className={`bg-white rounded-2xl border p-6 flex flex-col justify-between transition-all ${
                  isSelected
                    ? 'border-brand-500 ring-2 ring-brand-100 shadow-sm'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div>
                  <div className="flex justify-between items-start mb-3">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2.5 rounded-xl bg-brand-50 text-brand-600">
                        <Building2 className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="font-bold text-slate-900 text-base leading-tight">{agency.name}</h3>
                        <span className="text-xs text-slate-500 font-medium">{agency.city} {agency.district && `/ ${agency.district}`}</span>
                      </div>
                    </div>
                    {isSelected && (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                        <Check className="w-3 h-3" /> Aktif
                      </span>
                    )}
                  </div>

                  <div className="space-y-2 mt-4 text-xs text-slate-600 border-t border-slate-100 pt-3">
                    <div className="flex items-center gap-2">
                      <UserIcon className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span className="font-medium">{agency.contactPerson}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Phone className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{agency.phone}</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                      <span className="line-clamp-2 leading-relaxed">{agency.address}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-6 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  {!isSelected ? (
                    <button
                      onClick={() => selectAgency(agency.id)}
                      className="text-xs font-semibold text-brand-600 hover:text-brand-700 hover:underline"
                    >
                      Aktif Dükkan Yap
                    </button>
                  ) : (
                    <span className="text-xs text-slate-400 italic">Mevcut aktif bağlam</span>
                  )}

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => openEditModal(agency)}
                      className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                      title="Düzenle"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => handleDelete(agency.id, agency.name)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                      title="Sil"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-xl border border-slate-200 overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
              <h3 className="font-bold text-slate-900 text-lg">
                {editingId ? 'Dükkan Bilgilerini Güncelle' : 'Yeni Dükkan / Şube Ekle'}
              </h3>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-lg leading-none"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {errorMessage && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Dükkan / Şube Adı</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Örn: Öz Yıldız Rent a Car"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Şehir</label>
                  <input
                    type="text"
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Örn: İstanbul"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">İlçe (Opsiyonel)</label>
                  <input
                    type="text"
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    placeholder="Örn: Kadıköy"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">İletişim Telefonu</label>
                  <input
                    type="tel"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="0532 111 22 33"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Yetkili / Sorumlu Kişi</label>
                  <input
                    type="text"
                    required
                    value={contactPerson}
                    onChange={(e) => setContactPerson(e.target.value)}
                    placeholder="Örn: Ahmet Yılmaz"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Açık Adres</label>
                <textarea
                  required
                  rows={3}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Diğer dükkanların aracı teslim alabileceği veya ulaşabileceği tam adres"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white resize-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 border border-slate-200 text-slate-600 rounded-xl text-sm font-medium hover:bg-slate-50"
                >
                  İptal
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-xl text-sm font-semibold shadow-sm disabled:opacity-50"
                >
                  {submitting ? 'Kaydediliyor...' : editingId ? 'Güncelle' : 'Kaydet'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

