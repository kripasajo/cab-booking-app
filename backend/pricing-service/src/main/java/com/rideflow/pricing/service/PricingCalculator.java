package com.rideflow.pricing.service;

import com.rideflow.common.dto.FareEstimate;
import com.rideflow.common.util.GeoUtils;

/**
 * Pure fare maths, no Spring or Redis, so it is easy to unit test and to explain.
 *
 * fare = (base + perKm * km + perMinute * minutes) * surge
 * surge = (demand + 1) / (supply + 1), clamped to 1.0x .. 2.5x
 */
public final class PricingCalculator {

    static final double AVERAGE_SPEED_KMPH = 25.0;
    static final double MAX_SURGE = 2.5;

    private PricingCalculator() {
    }

    /**
     * @param demand active rides near the pickup
     * @param supply available drivers near the pickup
     */
    public static double surgeMultiplier(int demand, int supply) {
        double ratio = (demand + 1.0) / (supply + 1.0);
        double clamped = Math.max(1.0, Math.min(MAX_SURGE, ratio));
        return Math.round(clamped * 10) / 10.0;
    }

    public static FareEstimate estimate(double distanceKm, RideType type, int supply, int demand) {
        int minutes = Math.max(1, (int) Math.round(distanceKm / AVERAGE_SPEED_KMPH * 60));
        double baseFare = type.baseFare + type.perKm * distanceKm + type.perMinute * minutes;
        double surge = surgeMultiplier(demand, supply);
        double fare = Math.round(baseFare * surge);
        return new FareEstimate(type.name(), GeoUtils.round(distanceKm, 2), minutes,
                Math.round(baseFare), surge, fare, supply, demand);
    }
}
