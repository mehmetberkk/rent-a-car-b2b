import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Car, Building2, Globe2, LogOut, User as UserIcon } from 'lucide-react';

interface NavbarProps {
  agencySwitcher?: React.ReactNode;
}

export const Navbar: React.FC<NavbarProps> = ({ agencySwitcher }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const userJson = localStorage.getItem('user');
  const user = userJson ? JSON.parse(userJson) : null;

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('selectedAgencyId');
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <div className="flex items-center gap-8">
            <Link to="/" className="flex items-center gap-2 font-bold text-xl text-slate-800">
              <div className="bg-brand-600 p-2 rounded-lg text-white">
                <Car className="w-5 h-5" />
              </div>
              <span>RentA<span className="text-brand-600">Car</span> B2B</span>
            </Link>

            {user && (
              <nav className="hidden md:flex items-center gap-1">
                <Link
                  to="/fleet"
                  className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive('/fleet')
                      ? 'bg-brand-50 text-brand-700'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Car className="w-4 h-4" />
                  Filo Yönetimi
                </Link>
                <Link
                  to="/pool"
                  className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive('/pool')
                      ? 'bg-brand-50 text-brand-700'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Globe2 className="w-4 h-4" />
                  Ortak Araç Havuzu
                </Link>
                <Link
                  to="/agencies"
                  className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive('/agencies')
                      ? 'bg-brand-50 text-brand-700'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Building2 className="w-4 h-4" />
                  Dükkanlarım
                </Link>
              </nav>
            )}
          </div>

          <div className="flex items-center gap-4">
            {user ? (
              <>
                {agencySwitcher}
                <div className="flex items-center gap-3 pl-2 border-l border-slate-200">
                  <div className="flex items-center gap-2 text-sm text-slate-700 font-medium">
                    <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-600">
                      <UserIcon className="w-4 h-4" />
                    </div>
                    <span className="hidden sm:inline">{user.firstName} {user.lastName}</span>
                  </div>
                  <button
                    onClick={handleLogout}
                    title="Çıkış Yap"
                    className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                  >
                    <LogOut className="w-4 h-4" />
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-medium text-slate-700 hover:text-brand-600"
                >
                  Giriş Yap
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 rounded-lg transition-colors shadow-sm"
                >
                  Kayıt Ol
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

