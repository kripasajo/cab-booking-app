package com.rideflow.driver.model;

import java.util.Map;

/**
 * A driver's live state, stored in the Redis hash "driver:{id}".
 *
 * @param status OFFLINE, AVAILABLE or BUSY
 * @param phase  while BUSY: TO_PICKUP, TO_DROP or COMPLETING
 */
public record DriverState(
        String driverId,
        String name,
        String vehicle,
        String status,
        Long bookingId,
        String phase,
        double lat,
        double lng,
        Double pickupLat,
        Double pickupLng,
        Double dropLat,
        Double dropLng) {

    public static final String OFFLINE = "OFFLINE";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String BUSY = "BUSY";

    public boolean isAvailable() {
        return AVAILABLE.equals(status);
    }

    public boolean isBusy() {
        return BUSY.equals(status);
    }

    public boolean isOffline() {
        return OFFLINE.equals(status);
    }

    public DriverState withPosition(double newLat, double newLng) {
        return new DriverState(driverId, name, vehicle, status, bookingId, phase, newLat, newLng,
                pickupLat, pickupLng, dropLat, dropLng);
    }

    public static DriverState offline(String driverId, String name, String vehicle) {
        return new DriverState(driverId, name, vehicle, OFFLINE, null, null, 0, 0, null, null, null, null);
    }

    public static DriverState fromHash(String driverId, Map<Object, Object> h) {
        return new DriverState(
                driverId,
                str(h, "name"),
                str(h, "vehicle"),
                str(h, "status"),
                h.containsKey("bookingId") ? Long.valueOf(str(h, "bookingId")) : null,
                str(h, "phase"),
                num(h, "lat") == null ? 0 : num(h, "lat"),
                num(h, "lng") == null ? 0 : num(h, "lng"),
                num(h, "pickupLat"),
                num(h, "pickupLng"),
                num(h, "dropLat"),
                num(h, "dropLng"));
    }

    private static String str(Map<Object, Object> h, String field) {
        Object v = h.get(field);
        return v == null ? null : v.toString();
    }

    private static Double num(Map<Object, Object> h, String field) {
        Object v = h.get(field);
        return v == null ? null : Double.valueOf(v.toString());
    }
}
