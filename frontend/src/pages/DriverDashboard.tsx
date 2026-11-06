import { useEffect, useRef, useState } from 'react';
import { api, post } from '../api';
import { useAuth } from '../auth';
import MapView from '../components/MapView';
import type { MapMarker } from '../components/markers';
import { CITY_CENTER } from '../config';
import { coords, distanceKm, etaMinutes, rupees, stepTowards } from '../geo';
import { ACTIVE_STATUSES, type DriverState, type LatLng, type Ride, type RideEvent } from '../types';
import { connectWs, useTopic } from '../ws';

/** How far the car moves per second when "Drive there for me" is on (demo speed). */
const AUTO_DRIVE_STEP_KM = 0.08;

export default function DriverDashboard() {
  const { auth } = useAuth();
  const [me, setMe] = useState<DriverState | null>(null);
  const [position, setPosition] = useState<LatLng>(CITY_CENTER);
  const [mapCenter, setMapCenter] = useState<LatLng>(CITY_CENTER);
  const [ride, setRide] = useState<Ride | null>(null);
  const [autoDrive, setAutoDrive] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  // refs let timers read the latest values without restarting
  const positionRef = useRef(position);
  positionRef.current = position;
  const rideRef = useRef(ride);
  rideRef.current = ride;

  const online = !!me && me.status !== 'OFFLINE';
  const rideActive = !!ride && ACTIVE_STATUSES.includes(ride.status);

  const flash = (text: string) => {
    setNotice(text);
    window.setTimeout(() => setNotice((current) => (current === text ? '' : current)), 5000);
  };

  // On load: open the WebSocket, read our status from Redis and restore any ride in progress.
  useEffect(() => {
    connectWs();
    (async () => {
      const state = await api<DriverState>('/api/drivers/me');
      setMe(state);
      if (state.status !== 'OFFLINE' && (state.lat || state.lng)) {
        const p = { lat: state.lat, lng: state.lng };
        setPosition(p);
        setMapCenter(p);
      }
      const active = await api<Ride | undefined>('/api/bookings/active');
      if (active) setRide(active);
    })().catch((e) => setError(e.message));
  }, []);

  // New ride offers and cancellations arrive on our personal topic.
  useTopic<RideEvent>(auth ? `/topic/drivers/${auth.user.id}` : null, (e) => {
    if (e.type === 'DRIVER_ASSIGNED') {
      api<Ride>(`/api/bookings/${e.bookingId}`)
        .then((r) => {
          setRide(r);
          setMe((m) => (m ? { ...m, status: 'BUSY', bookingId: r.id } : m));
          flash(`New ride: pick up ${r.riderName}`);
        })
        .catch(() => undefined);
    } else if (e.type === 'RIDE_CANCELLED') {
      setAutoDrive(false);
      setRide((r) => (r && r.id === e.bookingId ? { ...r, status: 'CANCELLED' } : r));
      setMe((m) => (m ? { ...m, status: 'AVAILABLE', bookingId: null } : m));
      flash(`${e.riderName} cancelled the ride`);
    }
  });

  // Safety net: every 5 s re-read status in case a WebSocket message was missed.
  useEffect(() => {
    if (!online) return;
    const timer = setInterval(async () => {
      try {
        const state = await api<DriverState>('/api/drivers/me');
        setMe(state);
        const current = rideRef.current;
        if (state.bookingId && (!current || current.id !== state.bookingId)) {
          setRide(await api<Ride>(`/api/bookings/${state.bookingId}`));
        } else if (current && ACTIVE_STATUSES.includes(current.status)) {
          setRide(await api<Ride>(`/api/bookings/${current.id}`));
        }
      } catch {
        // ignore, we'll try again
      }
    }, 5000);
    return () => clearInterval(timer);
  }, [online]);

  // "Drive there for me": move towards the pickup (or drop) once a second, reporting each position.
  const target: LatLng | null =
    ride?.status === 'DRIVER_ASSIGNED'
      ? { lat: ride.pickupLat, lng: ride.pickupLng }
      : ride?.status === 'IN_PROGRESS'
        ? { lat: ride.dropLat, lng: ride.dropLng }
        : null;

  useEffect(() => {
    if (!autoDrive || !target) return;
    const timer = setInterval(() => {
      const { position: next, arrived } = stepTowards(positionRef.current, target, AUTO_DRIVE_STEP_KM);
      setPosition(next);
      post('/api/drivers/location', next).catch(() => undefined);
      if (arrived) {
        setAutoDrive(false);
        flash(rideRef.current?.status === 'DRIVER_ASSIGNED' ? 'You’ve reached the pickup point' : 'You’ve reached the drop point');
      }
    }, 1000);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoDrive, target?.lat, target?.lng]);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const goOnline = () => run(async () => setMe(await post<DriverState>('/api/drivers/online', position)));
  const goOffline = () => run(async () => setMe(await post<DriverState>('/api/drivers/offline')));

  const startRide = () =>
    run(async () => {
      setAutoDrive(false);
      setRide(await post<Ride>(`/api/bookings/${ride!.id}/start`));
      flash('Trip started. Drive to the drop point.');
    });

  const completeRide = () =>
    run(async () => {
      setAutoDrive(false);
      setRide(await post<Ride>(`/api/bookings/${ride!.id}/complete`));
    });

  const backToWaiting = () => {
    setRide(null);
    // driver-service frees us when it consumes the RIDE_COMPLETED / RIDE_CANCELLED event
    window.setTimeout(() => api<DriverState>('/api/drivers/me').then(setMe).catch(() => undefined), 800);
  };

  const onMapClick = (p: LatLng) => {
    if (autoDrive) return;
    setPosition(p);
    if (online) post('/api/drivers/location', p).catch((e) => setError(e.message));
  };

  // ----- map content -----
  const markers: MapMarker[] = [{ id: 'me', position, kind: 'self', title: 'You' }];
  if (ride && rideActive) {
    markers.push({ id: 'pickup', position: { lat: ride.pickupLat, lng: ride.pickupLng }, kind: 'pickup', title: 'Pickup' });
    markers.push({ id: 'drop', position: { lat: ride.dropLat, lng: ride.dropLng }, kind: 'drop', title: 'Drop' });
  }
  const route = target ? [position, target] : undefined;
  const kmToTarget = target ? distanceKm(position, target) : 0;

  return (
    <div className="dashboard">
      <aside className="sheet">
        {notice && <p className="notice" role="status">{notice}</p>}

        {!rideActive && ride?.status === 'COMPLETED' && (
          <div className="sheet-section">
            <h2>Trip complete</h2>
            <p className="big-fare">{rupees(ride.fare)}</p>
            <p className="muted">Earned from {ride.riderName}’s {ride.distanceKm.toFixed(1)} km trip.</p>
            <button className="btn primary wide" onClick={backToWaiting}>Take the next ride</button>
          </div>
        )}

        {!rideActive && ride?.status === 'CANCELLED' && (
          <div className="sheet-section">
            <h2>Ride cancelled</h2>
            <p className="muted">{ride.riderName} cancelled. You’re back in the queue for new rides.</p>
            <button className="btn primary wide" onClick={backToWaiting}>OK</button>
          </div>
        )}

        {ride && rideActive && (
          <div className="sheet-section">
            <h2>{ride.status === 'DRIVER_ASSIGNED' ? `Pick up ${ride.riderName}` : `Drop off ${ride.riderName}`}</h2>
            <p className="eta">
              {kmToTarget.toFixed(1)} km to the {ride.status === 'DRIVER_ASSIGNED' ? 'pickup' : 'drop'} point · about {etaMinutes(kmToTarget)} min
            </p>
            <dl className="facts">
              <div><dt>Fare</dt><dd>{rupees(ride.fare)}</dd></div>
              <div><dt>Trip</dt><dd>{ride.distanceKm.toFixed(1)} km</dd></div>
              <div><dt>Type</dt><dd>{ride.rideType}</dd></div>
            </dl>
            <label className="toggle">
              <input type="checkbox" checked={autoDrive} onChange={(e) => setAutoDrive(e.target.checked)} />
              <span>Drive there for me (simulates GPS updates)</span>
            </label>
            {ride.status === 'DRIVER_ASSIGNED' ? (
              <button className="btn primary wide" disabled={busy} onClick={startRide}>Start trip</button>
            ) : (
              <button className="btn primary wide" disabled={busy} onClick={completeRide}>Complete trip</button>
            )}
          </div>
        )}

        {!ride && (
          <div className="sheet-section">
            <h2>{online ? 'You’re online' : 'You’re offline'}</h2>
            <p className="muted">
              {online
                ? 'Waiting for a ride. The nearest free driver to a rider gets the trip automatically.'
                : 'Go online to start receiving rides.'}
            </p>
            <div className="driver-card">
              <div className="avatar" aria-hidden="true">{auth?.user.name.charAt(0)}</div>
              <div>
                <div className="driver-name">{auth?.user.name}</div>
                <div className="muted small">{auth?.user.vehicle}</div>
              </div>
            </div>
            <p className="hint">Your position: {coords(position)}. Click the map to move.</p>
            {online ? (
              <button className="btn ghost wide" disabled={busy} onClick={goOffline}>Go offline</button>
            ) : (
              <button className="btn primary wide" disabled={busy || !me} onClick={goOnline}>Go online</button>
            )}
          </div>
        )}

        {error && <p className="error" role="alert">{error}</p>}
      </aside>

      <section className="map-area">
        <MapView center={mapCenter} markers={markers} route={route} onMapClick={onMapClick} />
      </section>
    </div>
  );
}
