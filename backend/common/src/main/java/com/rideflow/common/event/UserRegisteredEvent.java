package com.rideflow.common.event;

/** A new account was created. Published by user-service on topic "user-events". */
public record UserRegisteredEvent(
        Long userId,
        String name,
        String email,
        String role,
        long timestamp) {
}
