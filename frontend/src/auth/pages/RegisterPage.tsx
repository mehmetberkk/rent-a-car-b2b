import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../services/authApi';
import { Car, Mail, Lock, User as UserIcon, CheckCircle2, AlertCircle } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setLoading(true);

    try {
      await authApi.register({ firstName, lastName, email, password });
      setSuccess(true);
    } catch (err: any) {
      const detail = err.response?.data?.detail || 'Kayıt sırasında bir hata oluştu.';
      setErrorMessage(detail);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-slate-200 p-8 text-center">
          <div className="w-14 h-14 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 className="w-8 h-8" />
          </div>
          <h2 className="text-2xl font-bold text-slate-800 mb-2">Aktivasyon E-postası Gönderildi</h2>
          <p className="text-slate-600 text-sm mb-6 leading-relaxed">
            <strong>{email}</strong> adresinize bir doğrulama bağlantısı gönderildi. Lütfen e-postanızı kontrol ederek hesabınızı aktif ediniz.
          </p>
          <div className="bg-slate-50 p-4 rounded-xl border border-slate-100 text-xs text-slate-500 mb-6">
            Geliştirme ortamında (Mailpit/MailHog) e-postaları <span className="font-mono text-brand-600 font-bold">http://localhost:8025</span> adresinden inceleyebilirsiniz.
          </div>
          <Link
            to="/login"
            className="inline-block w-full py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-xl transition-colors shadow-sm"
          >
            Giriş Sayfasına Dön
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-brand-600 text-white rounded-xl mb-3 shadow-sm">
            <Car className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">B2B Platformuna Kayıt Olun</h1>
          <p className="text-sm text-slate-500 mt-1">Rent a car dükkan sahipleri ortak araç havuzu</p>
        </div>

        {errorMessage && (
          <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-3 text-sm text-rose-700">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">Ad</label>
              <div className="relative">
                <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <input
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder="Ahmet"
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">Soyad</label>
              <div className="relative">
                <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <input
                  type="text"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  placeholder="Yılmaz"
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">E-posta Adresi</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ahmet@example.com"
                className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">Şifre</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="En az 8 karakter (büyük/küçük harf, rakam)"
                className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-xl transition-colors shadow-sm disabled:opacity-50"
          >
            {loading ? 'Kaydediliyor...' : 'Hesap Oluştur'}
          </button>
        </form>

        <p className="text-center text-xs text-slate-500 mt-6">
          Zaten bir hesabınız var mı?{' '}
          <Link to="/login" className="text-brand-600 font-semibold hover:underline">
            Giriş Yapın
          </Link>
        </p>
      </div>
    </div>
  );
};

