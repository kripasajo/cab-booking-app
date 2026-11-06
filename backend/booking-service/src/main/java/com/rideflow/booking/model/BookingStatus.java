package com.rideflow.booking.model;

import java.util.List;

/**
 * Ride lifecycle:
 * REQUESTED -> DRIVER_ASSIGNED -> IN_PROGRESS -> COMPLETED
 * REQUESTED -> NO_DRIVER_FOUND
 * REQUESTED / DRIVER_ASSIGNED -> CANCELLED
 */
public enum BookingStatus {
    REQUESTED,
    DRIVER_ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_DRIVER_FOUND;

    public static final List<BookingStatus> ACTIVE = List.of(REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS);
}
