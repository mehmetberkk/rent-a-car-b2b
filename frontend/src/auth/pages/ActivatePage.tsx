import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../services/authApi';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';

export const ActivatePage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      setErrorMessage('Geçersiz aktivasyon bağlantısı. Jeton (token) parametresi eksik.');
      return;
    }

    const doActivate = async () => {
      try {
        await authApi.activate(token);
        setSuccess(true);
      } catch (err: any) {
        setErrorMessage(
          err.response?.data?.detail || 'Aktivasyon işlemi başarısız oldu veya jetonun süresi dolmuş.'
        );
      } finally {
        setLoading(false);
      }
    };

    doActivate();
  }, [token]);

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-slate-200 p-8 text-center">
        {loading ? (
          <div className="py-8">
            <Loader2 className="w-10 h-10 text-brand-600 animate-spin mx-auto mb-4" />
            <h2 className="text-xl font-bold text-slate-800">Hesabınız Doğrulanıyor</h2>
            <p className="text-sm text-slate-500 mt-2">Lütfen bekleyiniz...</p>
          </div>
        ) : success ? (
          <div>
            <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-10 h-10" />
            </div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Hesabınız Başarıyla Aktive Edildi!</h2>
            <p className="text-sm text-slate-600 mb-6">
              Artık rent a car dükkanlarınızı ekleyebilir, filonuzu yönetebilir ve ortak araç havuzuna erişebilirsiniz.
            </p>
            <Link
              to="/login"
              className="inline-block w-full py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white font-medium rounded-xl transition-colors shadow-sm"
            >
              Giriş Yap
            </Link>
          </div>
        ) : (
          <div>
            <div className="w-16 h-16 bg-rose-100 text-rose-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <XCircle className="w-10 h-10" />
            </div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Aktivasyon Başarısız</h2>
            <p className="text-sm text-slate-600 mb-6">{errorMessage}</p>
            <Link
              to="/login"
              className="inline-block w-full py-2.5 px-4 bg-slate-800 hover:bg-slate-900 text-white font-medium rounded-xl transition-colors shadow-sm"
            >
              Giriş Ekranına Git
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

