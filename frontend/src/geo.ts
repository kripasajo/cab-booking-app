import type { LatLng } from './types';

/** Haversine distance in km (same formula as the backend's GeoUtils). */
export function distanceKm(a: LatLng, b: LatLng): number {
  const R = 6371;
  const dLat = ((b.lat - a.lat) * Math.PI) / 180;
  const dLng = ((b.lng - a.lng) * Math.PI) / 180;
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((a.lat * Math.PI) / 180) * Math.cos((b.lat * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
}

/** Moves `stepKm` from `from` towards `to` in a straight line. */
export function stepTowards(from: LatLng, to: LatLng, stepKm: number): { position: LatLng; arrived: boolean } {
  const remaining = distanceKm(from, to);
  if (remaining <= stepKm || remaining < 0.01) return { position: to, arrived: true };
  const f = stepKm / remaining;
  return {
    position: { lat: from.lat + (to.lat - from.lat) * f, lng: from.lng + (to.lng - from.lng) * f },
    arrived: false,
  };
}

/** Rough city driving time at 25 km/h. */
export function etaMinutes(km: number): number {
  return Math.max(1, Math.round((km / 25) * 60));
}

export const rupees = (n: number) => `₹${Math.round(n).toLocaleString('en-IN')}`;

export const coords = (p: LatLng) => `${p.lat.toFixed(4)}, ${p.lng.toFixed(4)}`;
