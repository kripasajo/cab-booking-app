import type { ReactNode } from 'react';

/** Two-column frame for the login and sign-up screens. */
export default function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="auth-page">
      <section className="auth-hero" aria-hidden="true">
        <div className="auth-brand"><span className="brand-mark" />RideFlow</div>
        <svg className="auth-route" viewBox="0 0 400 300" preserveAspectRatio="xMidYMid meet">
          <path d="M40 250 C 120 250, 110 120, 200 130 S 300 40, 360 50" fill="none" stroke="rgba(255,255,255,.35)" strokeWidth="10" strokeLinecap="round" />
          <path className="auth-route-live" d="M40 250 C 120 250, 110 120, 200 130 S 300 40, 360 50" fill="none" stroke="#f2a900" strokeWidth="4" strokeLinecap="round" />
          <circle cx="40" cy="250" r="12" fill="#12805c" stroke="white" strokeWidth="4" />
          <circle cx="360" cy="50" r="12" fill="#d6336c" stroke="white" strokeWidth="4" />
        </svg>
        <p className="auth-tagline">Book a cab, watch it arrive, ride.</p>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <h1>{title}</h1>
          {children}
        </div>
      </section>
    </div>
  );
}
