package com.rideflow.driver.model;

public record NearbyDriver(String driverId, String name, String vehicle, double lat, double lng, double distanceKm) {
}
