import { useEffect, useState } from 'react';
import { api, post } from '../api';
import MapView from '../components/MapView';
import type { MapMarker } from '../components/markers';
import RideProgress from '../components/RideProgress';
import { CITY_CENTER } from '../config';
import { coords, distanceKm, etaMinutes, rupees } from '../geo';
import {
  ACTIVE_STATUSES,
  type DriverLocationEvent,
  type FareEstimate,
  type LatLng,
  type NearbyResponse,
  type Ride,
  type RideEvent,
} from '../types';
import { connectWs, useTopic } from '../ws';

const RIDE_TYPES = [
  { id: 'MINI', name: 'Mini', blurb: 'Hatchbacks, best price' },
  { id: 'SEDAN', name: 'Sedan', blurb: 'More legroom' },
  { id: 'SUV', name: 'SUV', blurb: 'Seats up to 6' },
];

type PickMode = 'pickup' | 'drop';

export default function RiderDashboard() {
  const [pickup, setPickup] = useState<LatLng | null>(null);
  const [drop, setDrop] = useState<LatLng | null>(null);
  const [mode, setMode] = useState<PickMode>('pickup');
  const [rideType, setRideType] = useState('MINI');
  const [estimates, setEstimates] = useState<Record<string, FareEstimate>>({});
  const [nearby, setNearby] = useState<NearbyResponse | null>(null);
  const [ride, setRide] = useState<Ride | null>(null);
  const [driverPos, setDriverPos] = useState<LatLng | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const isActive = !!ride && ACTIVE_STATUSES.includes(ride.status);
  const activeRideId = isActive ? ride!.id : null;

  // On load: open the WebSocket and restore a ride that is still going (e.g. after a page refresh).
  useEffect(() => {
    connectWs();
    api<Ride | undefined>('/api/bookings/active')
      .then((r) => r && setRide(r))
      .catch((e) => setError(e.message));
  }, []);

  // Nearby available drivers (Redis GEOSEARCH), refreshed every 4 s while not on a ride.
  const searchPoint = pickup ?? CITY_CENTER;
  useEffect(() => {
    if (isActive) return;
    let cancelled = false;
    const load = () =>
      api<NearbyResponse>(`/api/drivers/nearby?lat=${searchPoint.lat}&lng=${searchPoint.lng}&radiusKm=3`)
        .then((r) => !cancelled && setNearby(r))
        .catch(() => undefined);
    load();
    const timer = setInterval(load, 4000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [searchPoint.lat, searchPoint.lng, isActive]);

  // Fare for every ride type. Re-priced when the number of free drivers changes (surge).
  const supply = nearby?.drivers.length;
  useEffect(() => {
    if (!pickup || !drop || isActive) {
      setEstimates({});
      return;
    }
    let cancelled = false;
    const q = `pickupLat=${pickup.lat}&pickupLng=${pickup.lng}&dropLat=${drop.lat}&dropLng=${drop.lng}`;
    Promise.all(RIDE_TYPES.map((t) => api<FareEstimate>(`/api/pricing/estimate?${q}&rideType=${t.id}`)))
      .then((list) => !cancelled && setEstimates(Object.fromEntries(list.map((e) => [e.rideType, e]))))
      .catch((e) => !cancelled && setError(e.message));
    return () => {
      cancelled = true;
    };
  }, [pickup, drop, supply, isActive]);

  // Real-time updates over WebSocket: ride status + live driver position.
  useTopic<RideEvent>(activeRideId ? `/topic/rides/${activeRideId}` : null, (e) =>
    setRide((prev) =>
      prev && prev.id === e.bookingId
        ? { ...prev, status: e.status, driverId: e.driverId, driverName: e.driverName, vehicle: e.vehicle }
        : prev,
    ),
  );
  useTopic<DriverLocationEvent>(activeRideId ? `/topic/rides/${activeRideId}/location` : null, (e) =>
    setDriverPos({ lat: e.lat, lng: e.lng }),
  );

  // Safety net: re-sync from the server every 5 s in case a WebSocket message was missed.
  useEffect(() => {
    if (!activeRideId) return;
    const timer = setInterval(() => {
      api<Ride>(`/api/bookings/${activeRideId}`).then(setRide).catch(() => undefined);
    }, 5000);
    return () => clearInterval(timer);
  }, [activeRideId]);

  // Where is the driver right now? Asked once when a driver is assigned.
  const assignedDriver = isActive ? ride!.driverId : null;
  useEffect(() => {
    if (!assignedDriver) {
      setDriverPos(null);
      return;
    }
    api<LatLng>(`/api/drivers/${assignedDriver}/location`)
      .then((p) => setDriverPos((prev) => prev ?? { lat: p.lat, lng: p.lng }))
      .catch(() => undefined);
  }, [assignedDriver]);

  const onMapClick = (p: LatLng) => {
    if (ride) return;
    setError('');
    if (mode === 'pickup') {
      setPickup(p);
      setMode('drop');
    } else {
      setDrop(p);
    }
  };

  const useMyLocation = () => {
    if (!navigator.geolocation) {
      setError('Your browser cannot share its location. Click the map instead.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setPickup({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setMode('drop');
      },
      () => setError('Location access was blocked. Click the map to set your pickup.'),
    );
  };

  const book = async () => {
    if (!pickup || !drop) return;
    setBusy(true);
    setError('');
    try {
      const created = await post<Ride>('/api/bookings', {
        pickupLat: pickup.lat,
        pickupLng: pickup.lng,
        dropLat: drop.lat,
        dropLng: drop.lng,
        rideType,
      });
      setDriverPos(null);
      setRide(created);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    if (!ride) return;
    setBusy(true);
    try {
      setRide(await post<Ride>(`/api/bookings/${ride.id}/cancel`));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const rate = async (stars: number) => {
    if (!ride) return;
    try {
      setRide(await post<Ride>(`/api/bookings/${ride.id}/rate`, { rating: stars }));
    } catch (e) {
      setError((e as Error).message);
    }
  };

  /** Leave the finished ride screen. `keepRoute` re-uses the same pickup and drop. */
  const reset = (keepRoute: boolean) => {
    if (ride && keepRoute) {
      setPickup({ lat: ride.pickupLat, lng: ride.pickupLng });
      setDrop({ lat: ride.dropLat, lng: ride.dropLng });
    } else {
      setDrop(null);
      setMode('drop');
    }
    setRide(null);
    setDriverPos(null);
    setError('');
  };

  // ----- map content -----
  const shownPickup = ride ? { lat: ride.pickupLat, lng: ride.pickupLng } : pickup;
  const shownDrop = ride ? { lat: ride.dropLat, lng: ride.dropLng } : drop;
  const markers: MapMarker[] = [];
  if (!ride && nearby) {
    nearby.drivers.forEach((d) =>
      markers.push({ id: d.driverId, position: { lat: d.lat, lng: d.lng }, kind: 'car', title: `${d.name}, ${d.distanceKm} km away` }),
    );
  }
  if (shownPickup) markers.push({ id: 'pickup', position: shownPickup, kind: 'pickup', title: 'Pickup' });
  if (shownDrop) markers.push({ id: 'drop', position: shownDrop, kind: 'drop', title: 'Drop' });
  if (isActive && driverPos) markers.push({ id: 'driver', position: driverPos, kind: 'car', title: ride?.driverName ?? 'Driver' });
  const route = shownPickup && shownDrop ? [shownPickup, shownDrop] : undefined;

  return (
    <div className="dashboard">
      <aside className="sheet">
        {ride && isActive && (
          <ActiveRide ride={ride} driverPos={driverPos} busy={busy} onCancel={cancel} />
        )}
        {ride && ride.status === 'COMPLETED' && <CompletedRide ride={ride} onRate={rate} onDone={() => reset(false)} />}
        {ride && (ride.status === 'CANCELLED' || ride.status === 'NO_DRIVER_FOUND') && (
          <div className="sheet-section">
            <h2>{ride.status === 'CANCELLED' ? 'Ride cancelled' : 'No drivers nearby'}</h2>
            <p className="muted">
              {ride.status === 'CANCELLED'
                ? 'You were not charged.'
                : 'Every driver within 5 km is busy right now. Try again in a minute.'}
            </p>
            <div className="row">
              <button className="btn primary" onClick={() => reset(true)}>Book this trip again</button>
              <button className="btn ghost" onClick={() => reset(false)}>Plan a new trip</button>
            </div>
          </div>
        )}

        {!ride && (
          <>
            <div className="sheet-section">
              <h2>Where to?</h2>
              <div className="stops">
                <button className={`stop ${mode === 'pickup' ? 'picking' : ''}`} onClick={() => setMode('pickup')}>
                  <span className="stop-dot pickup" aria-hidden="true" />
                  <span className="stop-text">
                    <span className="stop-label">Pickup</span>
                    <span className="stop-value">{pickup ? coords(pickup) : 'Click the map'}</span>
                  </span>
                </button>
                <button className={`stop ${mode === 'drop' ? 'picking' : ''}`} onClick={() => setMode('drop')}>
                  <span className="stop-dot drop" aria-hidden="true" />
                  <span className="stop-text">
                    <span className="stop-label">Drop</span>
                    <span className="stop-value">{drop ? coords(drop) : 'Click the map'}</span>
                  </span>
                </button>
              </div>
              <p className="hint">
                Now choosing the <strong>{mode === 'pickup' ? 'pickup' : 'drop'}</strong> point.{' '}
                <button className="btn-link" onClick={useMyLocation}>Use my location as pickup</button>
              </p>
            </div>

            <div className="sheet-section">
              <p className="nearby">
                <span className="pulse" aria-hidden="true" />
                {nearby
                  ? `${nearby.drivers.length} driver${nearby.drivers.length === 1 ? '' : 's'} free within 3 km · found in ${nearby.lookupMs} ms`
                  : 'Looking for drivers near you…'}
              </p>
            </div>

            {pickup && drop && (
              <div className="sheet-section">
                <div className="ride-types" role="radiogroup" aria-label="Ride type">
                  {RIDE_TYPES.map((t) => {
                    const est = estimates[t.id];
                    return (
                      <button
                        key={t.id}
                        role="radio"
                        aria-checked={rideType === t.id}
                        className={`ride-type ${rideType === t.id ? 'on' : ''}`}
                        onClick={() => setRideType(t.id)}
                      >
                        <span className="rt-name">{t.name}</span>
                        <span className="rt-blurb">{t.blurb}</span>
                        <span className="rt-fare">{est ? rupees(est.fare) : '…'}</span>
                      </button>
                    );
                  })}
                </div>
                {estimates[rideType] && (
                  <p className="fare-meta">
                    {estimates[rideType].distanceKm} km · about {estimates[rideType].durationMin} min
                    {estimates[rideType].surgeMultiplier > 1 && (
                      <span className="surge-tag" title="Prices are higher because more people are booking than there are free drivers nearby">
                        {estimates[rideType].surgeMultiplier}× busy-time pricing
                      </span>
                    )}
                  </p>
                )}
                <button className="btn primary wide" disabled={busy || !estimates[rideType]} onClick={book}>
                  {busy ? 'Booking…' : `Book ${RIDE_TYPES.find((t) => t.id === rideType)?.name}`}
                </button>
              </div>
            )}
          </>
        )}
        {error && <p className="error" role="alert">{error}</p>}
      </aside>

      <section className="map-area">
        <MapView center={CITY_CENTER} markers={markers} route={route} onMapClick={onMapClick} />
      </section>
    </div>
  );
}

function ActiveRide({ ride, driverPos, busy, onCancel }: {
  ride: Ride;
  driverPos: LatLng | null;
  busy: boolean;
  onCancel: () => void;
}) {
  let detail: string | undefined;
  if (driverPos && ride.status === 'DRIVER_ASSIGNED') {
    const km = distanceKm(driverPos, { lat: ride.pickupLat, lng: ride.pickupLng });
    detail = `${km.toFixed(1)} km away · arrives in about ${etaMinutes(km)} min`;
  } else if (driverPos && ride.status === 'IN_PROGRESS') {
    const km = distanceKm(driverPos, { lat: ride.dropLat, lng: ride.dropLng });
    detail = `${km.toFixed(1)} km to go · about ${etaMinutes(km)} min`;
  } else if (ride.status === 'REQUESTED') {
    detail = 'Matching you with the nearest driver';
  }

  return (
    <div className="sheet-section">
      <h2>Your ride</h2>
      <RideProgress status={ride.status} detail={detail} />
      {ride.driverName && (
        <div className="driver-card">
          <div className="avatar" aria-hidden="true">{ride.driverName.charAt(0)}</div>
          <div>
            <div className="driver-name">{ride.driverName}</div>
            <div className="muted small">{ride.vehicle}</div>
          </div>
        </div>
      )}
      <dl className="facts">
        <div><dt>Fare</dt><dd>{rupees(ride.fare)}{ride.surgeMultiplier > 1 && <span className="surge-tag">{ride.surgeMultiplier}×</span>}</dd></div>
        <div><dt>Distance</dt><dd>{ride.distanceKm.toFixed(1)} km</dd></div>
        <div><dt>Type</dt><dd>{ride.rideType}</dd></div>
      </dl>
      {(ride.status === 'REQUESTED' || ride.status === 'DRIVER_ASSIGNED') && (
        <button className="btn danger-ghost wide" disabled={busy} onClick={onCancel}>Cancel ride</button>
      )}
    </div>
  );
}

function CompletedRide({ ride, onRate, onDone }: { ride: Ride; onRate: (n: number) => void; onDone: () => void }) {
  return (
    <div className="sheet-section">
      <RideProgress status="COMPLETED" />
      <h2>You’ve arrived</h2>
      <p className="big-fare">{rupees(ride.fare)}</p>
      <p className="muted">Paid for {ride.distanceKm.toFixed(1)} km with {ride.driverName}.</p>
      <div className="rating" role="group" aria-label="Rate your driver">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            className={`star ${ride.rating && n <= ride.rating ? 'on' : ''}`}
            aria-label={`${n} star${n > 1 ? 's' : ''}`}
            disabled={!!ride.rating}
            onClick={() => onRate(n)}
          >
            ★
          </button>
        ))}
      </div>
      <p className="muted small">{ride.rating ? 'Thanks for rating your driver.' : 'How was your driver?'}</p>
      <button className="btn primary wide" onClick={onDone}>Book another ride</button>
    </div>
  );
}
