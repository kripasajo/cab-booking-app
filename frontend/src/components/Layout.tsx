import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { disconnectWs } from '../ws';

export default function Layout({ children }: { children: ReactNode }) {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();
  const isDriver = auth?.user.role === 'DRIVER';

  const handleLogout = () => {
    disconnectWs();
    logout();
    navigate('/login');
  };

  return (
    <div className="shell">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-mark" aria-hidden="true" />
          RideFlow
        </NavLink>
        <nav className="nav">
          <NavLink to={isDriver ? '/driver' : '/rider'}>{isDriver ? 'Drive' : 'Ride'}</NavLink>
          <NavLink to="/history">{isDriver ? 'Trips & earnings' : 'Your trips'}</NavLink>
        </nav>
        <div className="who">
          <span className="who-name">{auth?.user.name}</span>
          <span className={`role-pill ${isDriver ? 'driver' : 'rider'}`}>{isDriver ? 'Driver' : 'Rider'}</span>
          <button className="btn-link" onClick={handleLogout}>Log out</button>
        </div>
      </header>
      <main className="content">{children}</main>
    </div>
  );
}
