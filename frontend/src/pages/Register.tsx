import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { post } from '../api';
import { useAuth } from '../auth';
import type { AuthResponse, Role } from '../types';
import AuthLayout from './AuthLayout';

export default function Register() {
  const { login } = useAuth();
  const [role, setRole] = useState<Role>('RIDER');
  const [form, setForm] = useState({ name: '', email: '', password: '', vehicle: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const update = (field: keyof typeof form) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      login(await post<AuthResponse>('/api/auth/register', { ...form, role }));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Create your account">
      <div className="segmented" role="radiogroup" aria-label="Account type">
        <button type="button" role="radio" aria-checked={role === 'RIDER'} className={role === 'RIDER' ? 'on' : ''} onClick={() => setRole('RIDER')}>I want to ride</button>
        <button type="button" role="radio" aria-checked={role === 'DRIVER'} className={role === 'DRIVER' ? 'on' : ''} onClick={() => setRole('DRIVER')}>I want to drive</button>
      </div>
      <form onSubmit={onSubmit} className="form">
        <label>
          Name
          <input value={form.name} onChange={update('name')} required autoComplete="name" />
        </label>
        <label>
          Email
          <input type="email" value={form.email} onChange={update('email')} required autoComplete="email" />
        </label>
        <label>
          Password
          <input type="password" value={form.password} onChange={update('password')} required minLength={6} autoComplete="new-password" />
        </label>
        {role === 'DRIVER' && (
          <label>
            Vehicle
            <input value={form.vehicle} onChange={update('vehicle')} placeholder="e.g. Swift Dzire · KA01AB1234" required />
          </label>
        )}
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn primary" disabled={loading}>{loading ? 'Creating account…' : 'Create account'}</button>
      </form>
      <p className="switch">Already have an account? <Link to="/login">Log in</Link></p>
    </AuthLayout>
  );
}
