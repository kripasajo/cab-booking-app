package com.rideflow.driver.dto;

import com.rideflow.driver.model.NearbyDriver;

import java.util.List;

/** Nearby drivers plus how long the Redis GEO lookup took. */
public record NearbyResponse(List<NearbyDriver> drivers, double lookupMs) {
}
