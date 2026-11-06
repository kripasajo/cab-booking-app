package com.rideflow.common.dto;

/** Returned by pricing-service and used by booking-service to price a ride. */
public record FareEstimate(
        String rideType,
        double distanceKm,
        int durationMin,
        double baseFare,
        double surgeMultiplier,
        double fare,
        int availableDrivers,
        int activeRides) {
}
