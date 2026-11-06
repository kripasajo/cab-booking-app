package com.rideflow.user.dto;

import com.rideflow.user.model.User;

public record UserDto(Long id, String name, String email, String role, String vehicle) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getVehicle());
    }
}
