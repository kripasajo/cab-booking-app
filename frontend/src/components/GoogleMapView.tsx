import { GoogleMap, MarkerF, PolylineF, useJsApiLoader } from '@react-google-maps/api';
import { GOOGLE_MAPS_KEY } from '../config';
import type { LatLng } from '../types';
import LiteMap from './LiteMap';
import { carIconUrl, COLORS, type MapMarker } from './markers';

interface Props {
  center: LatLng;
  markers: MapMarker[];
  route?: LatLng[];
  onMapClick?: (p: LatLng) => void;
}

const containerStyle = { width: '100%', height: '100%' };

function iconFor(marker: MapMarker): google.maps.Icon | google.maps.Symbol {
  if (marker.kind === 'car' || marker.kind === 'self') {
    return {
      url: carIconUrl(marker.kind),
      scaledSize: new google.maps.Size(36, 36),
      anchor: new google.maps.Point(18, 18),
    };
  }
  return {
    path: google.maps.SymbolPath.CIRCLE,
    scale: 9,
    fillColor: marker.kind === 'pickup' ? COLORS.pickup : COLORS.drop,
    fillOpacity: 1,
    strokeColor: '#ffffff',
    strokeWeight: 3,
  };
}

export default function GoogleMapView({ center, markers, route, onMapClick }: Props) {
  const { isLoaded, loadError } = useJsApiLoader({ id: 'rideflow-google-maps', googleMapsApiKey: GOOGLE_MAPS_KEY });

  if (loadError) {
    return (
      <LiteMap center={center} markers={markers} route={route} onMapClick={onMapClick}
        note="Google Maps failed to load (check the API key). Showing the simple map." />
    );
  }
  if (!isLoaded) return <div className="map-loading">Loading map…</div>;

  return (
    <GoogleMap
      mapContainerStyle={containerStyle}
      center={center}
      zoom={14}
      onClick={(e) => e.latLng && onMapClick?.({ lat: e.latLng.lat(), lng: e.latLng.lng() })}
      options={{ disableDefaultUI: true, zoomControl: true, clickableIcons: false, gestureHandling: 'greedy' }}
    >
      {route && route.length > 1 && (
        <PolylineF path={route} options={{ strokeColor: COLORS.route, strokeOpacity: 0.85, strokeWeight: 4 }} />
      )}
      {markers.map((m) => (
        <MarkerF key={m.id} position={m.position} icon={iconFor(m)} title={m.title} zIndex={m.kind === 'car' ? 1 : 2} />
      ))}
    </GoogleMap>
  );
}
