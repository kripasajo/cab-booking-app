import { GOOGLE_MAPS_KEY } from '../config';
import type { LatLng } from '../types';
import GoogleMapView from './GoogleMapView';
import LiteMap from './LiteMap';
import type { MapMarker } from './markers';

interface Props {
  center: LatLng;
  markers: MapMarker[];
  route?: LatLng[];
  onMapClick?: (p: LatLng) => void;
}

/** Google Maps when an API key is set, otherwise the built-in lite map. */
export default function MapView(props: Props) {
  return (
    <div className="map-frame">
      {GOOGLE_MAPS_KEY ? <GoogleMapView {...props} /> : <LiteMap {...props} />}
    </div>
  );
}
