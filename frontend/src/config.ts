import type { LatLng } from './types';

export const GOOGLE_MAPS_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? '';

export const CITY_CENTER: LatLng = {
  lat: Number(import.meta.env.VITE_CITY_CENTER_LAT ?? 12.9716),
  lng: Number(import.meta.env.VITE_CITY_CENTER_LNG ?? 77.5946),
};
