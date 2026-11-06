package com.rideflow.user.controller;

import com.rideflow.common.security.AuthUser;
import com.rideflow.user.dto.UserDto;
import com.rideflow.user.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /** Who am I? Requires a valid JWT. */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser user) {
        return authService.getUser(Long.valueOf(user.id()));
    }
}
