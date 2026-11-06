import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import Layout from './components/Layout';
import DriverDashboard from './pages/DriverDashboard';
import History from './pages/History';
import Login from './pages/Login';
import Register from './pages/Register';
import RiderDashboard from './pages/RiderDashboard';
import type { Role } from './types';

function homeFor(role: Role) {
  return role === 'DRIVER' ? '/driver' : '/rider';
}

function Protected({ role, children }: { role?: Role; children: ReactNode }) {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" replace />;
  if (role && auth.user.role !== role) return <Navigate to={homeFor(auth.user.role)} replace />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  const { auth } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={auth ? <Navigate to={homeFor(auth.user.role)} replace /> : <Login />} />
      <Route path="/register" element={auth ? <Navigate to={homeFor(auth.user.role)} replace /> : <Register />} />
      <Route path="/rider" element={<Protected role="RIDER"><RiderDashboard /></Protected>} />
      <Route path="/driver" element={<Protected role="DRIVER"><DriverDashboard /></Protected>} />
      <Route path="/history" element={<Protected><History /></Protected>} />
      <Route path="*" element={<Navigate to={auth ? homeFor(auth.user.role) : '/login'} replace />} />
    </Routes>
  );
}
