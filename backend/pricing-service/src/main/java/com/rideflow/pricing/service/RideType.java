package com.rideflow.pricing.service;

/** Fare card per vehicle class (INR). */
public enum RideType {
    MINI(40, 10, 1.0),
    SEDAN(60, 14, 1.5),
    SUV(90, 18, 2.0);

    final double baseFare;
    final double perKm;
    final double perMinute;

    RideType(double baseFare, double perKm, double perMinute) {
        this.baseFare = baseFare;
        this.perKm = perKm;
        this.perMinute = perMinute;
    }

    public static RideType parse(String value) {
        if (value == null || value.isBlank()) {
            return MINI;
        }
        try {
            return RideType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MINI;
        }
    }
}
