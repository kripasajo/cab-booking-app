package com.rideflow.common.event;

/**
 * A driver moved. Published by driver-service on topic "driver-locations".
 * {@code bookingId} is set while the driver is on a ride, so the rider can follow the car.
 */
public record DriverLocationEvent(
        String driverId,
        Long bookingId,
        double lat,
        double lng,
        long timestamp) {
}
