export type Role = 'RIDER' | 'DRIVER';

export interface LatLng {
  lat: number;
  lng: number;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  vehicle: string | null;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export type RideStatus =
  | 'REQUESTED'
  | 'DRIVER_ASSIGNED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_DRIVER_FOUND';

export const ACTIVE_STATUSES: RideStatus[] = ['REQUESTED', 'DRIVER_ASSIGNED', 'IN_PROGRESS'];

export interface Ride {
  id: number;
  riderId: string;
  riderName: string;
  driverId: string | null;
  driverName: string | null;
  vehicle: string | null;
  pickupLat: number;
  pickupLng: number;
  dropLat: number;
  dropLng: number;
  distanceKm: number;
  fare: number;
  surgeMultiplier: number;
  rideType: string;
  status: RideStatus;
  rating: number | null;
  createdAt: string;
  updatedAt: string;
}

/** Pushed over WebSocket on /topic/rides/{id} and /topic/drivers/{id} */
export interface RideEvent {
  type: string;
  bookingId: number;
  status: RideStatus;
  riderName: string;
  driverId: string | null;
  driverName: string | null;
  vehicle: string | null;
  fare: number;
}

/** Pushed over WebSocket on /topic/rides/{id}/location */
export interface DriverLocationEvent {
  driverId: string;
  bookingId: number | null;
  lat: number;
  lng: number;
}

export interface FareEstimate {
  rideType: string;
  distanceKm: number;
  durationMin: number;
  baseFare: number;
  surgeMultiplier: number;
  fare: number;
  availableDrivers: number;
  activeRides: number;
}

export interface NearbyDriver {
  driverId: string;
  name: string;
  vehicle: string;
  lat: number;
  lng: number;
  distanceKm: number;
}

export interface NearbyResponse {
  drivers: NearbyDriver[];
  lookupMs: number;
}

export interface DriverState {
  driverId: string;
  name: string;
  vehicle: string | null;
  status: 'OFFLINE' | 'AVAILABLE' | 'BUSY';
  bookingId: number | null;
  lat: number;
  lng: number;
}
