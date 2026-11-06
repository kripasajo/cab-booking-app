import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { rupees } from '../geo';
import type { Ride } from '../types';

const STATUS_TEXT: Record<string, string> = {
  REQUESTED: 'Finding driver',
  DRIVER_ASSIGNED: 'Driver on the way',
  IN_PROGRESS: 'On trip',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  NO_DRIVER_FOUND: 'No driver found',
};

export default function History() {
  const { auth } = useAuth();
  const isDriver = auth?.user.role === 'DRIVER';
  const [rides, setRides] = useState<Ride[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api<Ride[]>('/api/bookings/my').then(setRides).catch((e) => setError(e.message));
  }, []);

  const completed = rides?.filter((r) => r.status === 'COMPLETED') ?? [];
  const total = completed.reduce((sum, r) => sum + r.fare, 0);
  const rated = completed.filter((r) => r.rating);
  const avgRating = rated.length ? rated.reduce((s, r) => s + (r.rating ?? 0), 0) / rated.length : null;

  return (
    <div className="history">
      <div className="history-head">
        <h1>{isDriver ? 'Trips & earnings' : 'Your trips'}</h1>
        {rides && (
          <dl className="stats">
            <div><dt>Completed trips</dt><dd>{completed.length}</dd></div>
            <div><dt>{isDriver ? 'Earned' : 'Spent'}</dt><dd>{rupees(total)}</dd></div>
            {isDriver && <div><dt>Average rating</dt><dd>{avgRating ? `${avgRating.toFixed(1)} ★` : 'None yet'}</dd></div>}
          </dl>
        )}
      </div>

      {error && <p className="error">{error}</p>}
      {!rides && !error && <p className="muted">Loading trips…</p>}
      {rides && rides.length === 0 && (
        <p className="empty">No trips yet. {isDriver ? 'Go online on the Drive page to get ride requests.' : 'Book your first ride on the Ride page.'}</p>
      )}

      {rides && rides.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>{isDriver ? 'Rider' : 'Driver'}</th>
                <th>Type</th>
                <th>Distance</th>
                <th>Fare</th>
                <th>Status</th>
                <th>Rating</th>
              </tr>
            </thead>
            <tbody>
              {rides.map((r) => (
                <tr key={r.id}>
                  <td>{new Date(r.createdAt).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}</td>
                  <td>{isDriver ? r.riderName : r.driverName ?? 'None'}</td>
                  <td>{r.rideType}</td>
                  <td>{r.distanceKm.toFixed(1)} km</td>
                  <td>
                    {rupees(r.fare)}
                    {r.surgeMultiplier > 1 && <span className="surge-tag">{r.surgeMultiplier}×</span>}
                  </td>
                  <td><span className={`status status-${r.status.toLowerCase()}`}>{STATUS_TEXT[r.status]}</span></td>
                  <td className="stars">{r.rating ? '★'.repeat(r.rating) : ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
