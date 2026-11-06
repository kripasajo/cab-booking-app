import type { LatLng } from '../types';

export type MarkerKind = 'pickup' | 'drop' | 'car' | 'self';

export interface MapMarker {
  id: string;
  position: LatLng;
  kind: MarkerKind;
  title?: string;
}

export const COLORS = {
  pickup: '#12805c',
  drop: '#d6336c',
  route: '#1f4fd8',
};

/** Top-down car icon used by both map implementations. */
export function carSvg(body: string) {
  return `<svg xmlns='http://www.w3.org/2000/svg' width='36' height='36' viewBox='0 0 36 36'>
  <circle cx='18' cy='18' r='16' fill='white' stroke='#142033' stroke-width='2'/>
  <rect x='12' y='7' width='12' height='22' rx='4' fill='${body}' stroke='#142033' stroke-width='1.5'/>
  <rect x='13.5' y='11' width='9' height='4' rx='1.5' fill='#142033'/>
  <rect x='13.5' y='22' width='9' height='3' rx='1.2' fill='#142033'/>
</svg>`;
}

export function carIconUrl(kind: MarkerKind) {
  const body = kind === 'self' ? '#1f4fd8' : '#f2a900';
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(carSvg(body));
}
