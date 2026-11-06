import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { post } from '../api';
import { useAuth } from '../auth';
import type { AuthResponse } from '../types';
import AuthLayout from './AuthLayout';

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const signIn = async (e: string, p: string) => {
    setLoading(true);
    setError('');
    try {
      login(await post<AuthResponse>('/api/auth/login', { email: e, password: p }));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    signIn(email, password);
  };

  return (
    <AuthLayout title="Log in">
      <form onSubmit={onSubmit} className="form">
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn primary" disabled={loading}>{loading ? 'Logging in…' : 'Log in'}</button>
      </form>

      <div className="demo-box">
        <p>Try it with a demo account:</p>
        <div className="demo-buttons">
          <button className="btn ghost" disabled={loading} onClick={() => signIn('rider@demo.com', 'password123')}>Demo rider</button>
          <button className="btn ghost" disabled={loading} onClick={() => signIn('driver@demo.com', 'password123')}>Demo driver</button>
        </div>
      </div>

      <p className="switch">New here? <Link to="/register">Create an account</Link></p>
    </AuthLayout>
  );
}
