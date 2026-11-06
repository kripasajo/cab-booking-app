package com.rideflow.common.event;

/**
 * Result of matching a ride to a driver. Published by driver-service on topic "driver-assignments".
 * When {@code assigned} is false no driver was found nearby.
 */
public record DriverAssignmentEvent(
        Long bookingId,
        boolean assigned,
        String driverId,
        String driverName,
        String vehicle,
        double distanceKm,
        long timestamp) {
}
