package com.rideflow.common.kafka;

/** Kafka topic names, in one place so producers and consumers can't drift apart. */
public final class Topics {

    public static final String RIDE_EVENTS = "ride-events";
    public static final String DRIVER_ASSIGNMENTS = "driver-assignments";
    public static final String DRIVER_LOCATIONS = "driver-locations";
    public static final String USER_EVENTS = "user-events";

    private Topics() {
    }
}
