package com.rideflow.user.dto;

public record AuthResponse(String token, UserDto user) {
}
