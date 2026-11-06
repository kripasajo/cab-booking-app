package com.rideflow.driver.controller;

import com.rideflow.common.security.AuthUser;
import com.rideflow.driver.dto.LocationRequest;
import com.rideflow.driver.dto.NearbyResponse;
import com.rideflow.driver.model.DriverState;
import com.rideflow.driver.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverState me(@AuthenticationPrincipal AuthUser user) {
        return driverService.me(user);
    }

    @PostMapping("/online")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverState online(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LocationRequest req) {
        return driverService.goOnline(user, req.lat(), req.lng());
    }

    @PostMapping("/offline")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverState offline(@AuthenticationPrincipal AuthUser user) {
        return driverService.goOffline(user.id());
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverState location(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LocationRequest req) {
        return driverService.updateLocation(user.id(), req.lat(), req.lng());
    }

    /** Available drivers around a point, nearest first (Redis GEOSEARCH). */
    @GetMapping("/nearby")
    public NearbyResponse nearby(@RequestParam double lat, @RequestParam double lng,
                                 @RequestParam(defaultValue = "3") double radiusKm) {
        return driverService.nearby(lat, lng, Math.min(radiusKm, 20));
    }

    /** Last known position of a driver (used by the rider app right after assignment). */
    @GetMapping("/{driverId}/location")
    public Map<String, Object> driverLocation(@PathVariable String driverId) {
        DriverState state = driverService.location(driverId);
        return Map.of("driverId", driverId, "lat", state.lat(), "lng", state.lng());
    }
}
