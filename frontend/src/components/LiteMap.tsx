import type { MouseEvent } from 'react';
import type { LatLng } from '../types';
import { carIconUrl, COLORS, type MapMarker } from './markers';

interface Props {
  center: LatLng;
  markers: MapMarker[];
  route?: LatLng[];
  onMapClick?: (p: LatLng) => void;
  note?: string;
}

/** Degrees shown on each side of the centre (~5 km). */
const SPAN = 0.045;

function toPercent(p: LatLng, c: LatLng) {
  return {
    x: ((p.lng - (c.lng - SPAN)) / (2 * SPAN)) * 100,
    y: ((c.lat + SPAN - p.lat) / (2 * SPAN)) * 100,
  };
}

/**
 * A dependency-free fallback map used when no Google Maps API key is configured.
 * Same props as the Google map: markers, a route line and click-to-pick.
 */
export default function LiteMap({ center, markers, route, onMapClick, note }: Props) {
  const handleClick = (e: MouseEvent<HTMLDivElement>) => {
    if (!onMapClick) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const y = (e.clientY - rect.top) / rect.height;
    onMapClick({ lat: center.lat + SPAN - y * 2 * SPAN, lng: center.lng - SPAN + x * 2 * SPAN });
  };

  const line = route && route.length > 1 ? route.map((p) => toPercent(p, center)) : null;

  return (
    <div className="lite-map" onClick={handleClick} role="application" aria-label="Map. Click to choose a location.">
      {line && (
        <svg className="lite-route" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <polyline
            points={line.map((p) => `${p.x},${p.y}`).join(' ')}
            fill="none"
            stroke={COLORS.route}
            strokeWidth="3"
            strokeDasharray="8 6"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
      )}
      {markers.map((m) => {
        const { x, y } = toPercent(m.position, center);
        if (x < 0 || x > 100 || y < 0 || y > 100) return null;
        const style = { left: `${x}%`, top: `${y}%` };
        if (m.kind === 'car' || m.kind === 'self') {
          return <img key={m.id} className="lite-car" style={style} src={carIconUrl(m.kind)} alt={m.title ?? 'Car'} title={m.title} />;
        }
        return (
          <span key={m.id} className="lite-pin" title={m.title}
            style={{ ...style, background: m.kind === 'pickup' ? COLORS.pickup : COLORS.drop }} />
        );
      })}
      <div className="lite-note">{note ?? 'Simple map. Add a Google Maps API key to see real streets.'}</div>
    </div>
  );
}
