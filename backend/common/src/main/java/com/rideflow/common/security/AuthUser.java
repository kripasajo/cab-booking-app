package com.rideflow.common.security;

/**
 * The logged-in user, rebuilt from the JWT on every request.
 * Controllers get it with {@code @AuthenticationPrincipal AuthUser user}.
 *
 * @param id      user id (JWT "sub" claim)
 * @param name    display name
 * @param role    RIDER or DRIVER
 * @param vehicle vehicle description, only for drivers
 */
public record AuthUser(String id, String name, String role, String vehicle) {

    public boolean isRider() {
        return "RIDER".equals(role);
    }

    public boolean isDriver() {
        return "DRIVER".equals(role);
    }
}
