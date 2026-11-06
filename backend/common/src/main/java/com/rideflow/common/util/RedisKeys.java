package com.rideflow.common.util;

/**
 * Redis keys shared between driver-service (writes driver positions)
 * and pricing-service (reads them to calculate surge).
 */
public final class RedisKeys {

    /** GEO set: every online driver's last position. */
    public static final String DRIVERS_ALL = "drivers:locations";
    /** GEO set: only drivers that are free to take a ride. Used for nearest-driver lookup. */
    public static final String DRIVERS_AVAILABLE = "drivers:available";
    /** GEO set: pickup points of rides that are requested or in progress (demand). */
    public static final String RIDES_ACTIVE = "rides:active";

    /** Hash with a driver's status, name, vehicle and current ride. */
    public static String driver(String driverId) {
        return "driver:" + driverId;
    }

    private RedisKeys() {
    }
}
