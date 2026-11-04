import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/authApi';
import { Car, Mail, Lock, AlertCircle, CheckCircle2 } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isUnactivated, setIsUnactivated] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setIsUnactivated(false);
    setLoading(true);

    try {
      await login({ email, password });
      navigate('/fleet');
    } catch (err: any) {
      const detail = err.response?.data?.detail || 'Giriş yapılamadı. Bilgilerinizi kontrol ediniz.';
      setErrorMessage(detail);
      if (err.response?.status === 403 || detail.includes('aktive edilmemiştir')) {
        setIsUnactivated(true);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendActivation = async () => {
    setResendLoading(true);
    try {
      await authApi.resendActivation(email);
      setResendSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.detail || 'Aktivasyon e-postası tekrar gönderilemedi.');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-brand-600 text-white rounded-xl mb-3 shadow-sm">
            <Car className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">B2B Filo & Havuz Girişi</h1>
          <p className="text-sm text-slate-500 mt-1">Dükkan sahibi hesabınızla giriş yapın</p>
        </div>

        {errorMessage && (
          <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-xl text-sm text-rose-700">
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
              <div>
                <p>{errorMessage}</p>
                {isUnactivated && (
                  <div className="mt-3">
                    {resendSuccess ? (
                      <div className="flex items-center gap-1.5 text-xs text-emerald-700 font-medium">
                        <CheckCircle2 className="w-4 h-4" />
                        Yeni aktivasyon e-postası gönderildi. Lütfen gelen kutunuzu kontrol ediniz.
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={handleResendActivation}
                        disabled={resendLoading}
                        className="text-xs font-semibold text-brand-700 underline hover:text-brand-800"
                      >
                        {resendLoading ? 'Gönderiliyor...' : 'Yeni aktivasyon e-postası gönder'}
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
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
                placeholder="Şifreniz"
                className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-xl transition-colors shadow-sm disabled:opacity-50"
          >
            {loading ? 'Giriş Yapılıyor...' : 'Giriş Yap'}
          </button>
        </form>

        <p className="text-center text-xs text-slate-500 mt-6">
          Henüz hesabınız yok mu?{' '}
          <Link to="/register" className="text-brand-600 font-semibold hover:underline">
            Kayıt Olun
          </Link>
        </p>
      </div>
    </div>
  );
};

