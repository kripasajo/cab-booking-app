package com.rideflow.common.event;

/** Every step of a ride's life. Published by booking-service on topic "ride-events". */
public enum RideEventType {
    RIDE_REQUESTED,
    DRIVER_ASSIGNED,
    NO_DRIVER_FOUND,
    RIDE_STARTED,
    RIDE_COMPLETED,
    RIDE_CANCELLED,
    /** Internal: a driver was matched to a ride that had already been cancelled, so free the driver. */
    DRIVER_RELEASED
}
