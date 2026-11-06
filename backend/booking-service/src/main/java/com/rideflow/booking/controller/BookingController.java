package com.rideflow.booking.controller;

import com.rideflow.booking.dto.BookingDto;
import com.rideflow.booking.dto.CreateBookingRequest;
import com.rideflow.booking.dto.RateRequest;
import com.rideflow.booking.service.BookingService;
import com.rideflow.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RIDER')")
    public BookingDto create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateBookingRequest req) {
        return bookingService.create(user, req);
    }

    /** The caller's current ride (as rider or driver), or 204 if there is none. */
    @GetMapping("/active")
    public ResponseEntity<BookingDto> active(@AuthenticationPrincipal AuthUser user) {
        return bookingService.active(user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/my")
    public List<BookingDto> history(@AuthenticationPrincipal AuthUser user) {
        return bookingService.history(user);
    }

    @GetMapping("/{id}")
    public BookingDto get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return bookingService.get(user, id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RIDER')")
    public BookingDto cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return bookingService.cancel(user, id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public BookingDto start(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return bookingService.start(user, id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public BookingDto complete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return bookingService.complete(user, id);
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasRole('RIDER')")
    public BookingDto rate(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                           @Valid @RequestBody RateRequest req) {
        return bookingService.rate(user, id, req.rating());
    }
}
