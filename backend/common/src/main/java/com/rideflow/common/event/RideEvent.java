package com.rideflow.common.event;

/**
 * A snapshot of a booking at the moment something happened to it.
 * Carrying the full snapshot means consumers never need to call booking-service back.
 */
public record RideEvent(
        RideEventType type,
        Long bookingId,
        String status,
        String riderId,
        String riderName,
        String driverId,
        String driverName,
        String vehicle,
        double pickupLat,
        double pickupLng,
        double dropLat,
        double dropLng,
        double distanceKm,
        double fare,
        double surgeMultiplier,
        String rideType,
        long timestamp) {
}
