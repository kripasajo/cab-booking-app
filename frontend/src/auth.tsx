import { createContext, useContext, useState, type ReactNode } from 'react';
import type { AuthResponse } from './types';

// sessionStorage (not localStorage) so a rider and a driver can be logged in
// in two tabs of the same browser at the same time.
const STORAGE_KEY = 'rideflow.auth';

export function loadAuth(): AuthResponse | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  return raw ? (JSON.parse(raw) as AuthResponse) : null;
}

export function getToken(): string | null {
  return loadAuth()?.token ?? null;
}

export function clearAuth() {
  sessionStorage.removeItem(STORAGE_KEY);
}

interface AuthContextValue {
  auth: AuthResponse | null;
  login: (auth: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthResponse | null>(loadAuth);

  const login = (value: AuthResponse) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    setAuth(value);
  };

  const logout = () => {
    clearAuth();
    setAuth(null);
  };

  return <AuthContext.Provider value={{ auth, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
