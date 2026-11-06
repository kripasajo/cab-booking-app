package com.rideflow.booking.dto;

import com.rideflow.booking.model.Booking;

import java.time.Instant;

public record BookingDto(
        Long id,
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
        String status,
        Integer rating,
        Instant createdAt,
        Instant updatedAt) {

    public static BookingDto from(Booking b) {
        return new BookingDto(b.getId(), b.getRiderId(), b.getRiderName(), b.getDriverId(), b.getDriverName(),
                b.getVehicle(), b.getPickupLat(), b.getPickupLng(), b.getDropLat(), b.getDropLng(),
                b.getDistanceKm(), b.getFare(), b.getSurgeMultiplier(), b.getRideType(), b.getStatus().name(),
                b.getRating(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
