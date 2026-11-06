package com.rideflow.booking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record CreateBookingRequest(
        @DecimalMin("-90") @DecimalMax("90") double pickupLat,
        @DecimalMin("-180") @DecimalMax("180") double pickupLng,
        @DecimalMin("-90") @DecimalMax("90") double dropLat,
        @DecimalMin("-180") @DecimalMax("180") double dropLng,
        String rideType) {
}
