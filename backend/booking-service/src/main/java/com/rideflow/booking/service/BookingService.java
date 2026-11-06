package com.rideflow.booking.service;

import com.rideflow.booking.client.PricingClient;
import com.rideflow.booking.dto.BookingDto;
import com.rideflow.booking.dto.CreateBookingRequest;
import com.rideflow.booking.kafka.RideEventPublisher;
import com.rideflow.booking.model.Booking;
import com.rideflow.booking.model.BookingStatus;
import com.rideflow.booking.repository.BookingRepository;
import com.rideflow.common.dto.FareEstimate;
import com.rideflow.common.event.DriverAssignmentEvent;
import com.rideflow.common.event.RideEventType;
import com.rideflow.common.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * booking-service is the single owner of ride state.
 * Every change is saved first and then published as a RideEvent, so the database
 * is never behind what other services have been told.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookings;
    private final PricingClient pricingClient;
    private final RideEventPublisher publisher;

    public BookingService(BookingRepository bookings, PricingClient pricingClient, RideEventPublisher publisher) {
        this.bookings = bookings;
        this.pricingClient = pricingClient;
        this.publisher = publisher;
    }

    public BookingDto create(AuthUser rider, CreateBookingRequest req) {
        if (bookings.findFirstByRiderIdAndStatusInOrderByCreatedAtDesc(rider.id(), BookingStatus.ACTIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active ride");
        }
        String rideType = req.rideType() == null ? "MINI" : req.rideType().toUpperCase();
        FareEstimate estimate = pricingClient.estimate(
                req.pickupLat(), req.pickupLng(), req.dropLat(), req.dropLng(), rideType);

        Booking b = new Booking();
        b.setRiderId(rider.id());
        b.setRiderName(rider.name());
        b.setPickupLat(req.pickupLat());
        b.setPickupLng(req.pickupLng());
        b.setDropLat(req.dropLat());
        b.setDropLng(req.dropLng());
        b.setRideType(estimate.rideType());
        b.setDistanceKm(estimate.distanceKm());
        b.setFare(estimate.fare());
        b.setSurgeMultiplier(estimate.surgeMultiplier());
        b.setStatus(BookingStatus.REQUESTED);
        b = bookings.save(b);

        // driver-service reacts to this event and looks for the nearest driver
        publisher.publish(b, RideEventType.RIDE_REQUESTED);
        return BookingDto.from(b);
    }

    /** Called when driver-service has finished matching. */
    public void handleAssignment(DriverAssignmentEvent event) {
        Optional<Booking> found = bookings.findById(event.bookingId());
        if (found.isEmpty()) {
            log.warn("Assignment for unknown booking {}", event.bookingId());
            return;
        }
        Booking b = found.get();

        if (!event.assigned()) {
            if (b.getStatus() == BookingStatus.REQUESTED) {
                b.setStatus(BookingStatus.NO_DRIVER_FOUND);
                b = bookings.save(b);
                publisher.publish(b, RideEventType.NO_DRIVER_FOUND);
            }
            return;
        }

        if (b.getStatus() != BookingStatus.REQUESTED) {
            // The rider cancelled while we were matching: tell driver-service to free the driver again.
            publisher.publish(b, RideEventType.DRIVER_RELEASED, event.driverId());
            return;
        }

        b.setDriverId(event.driverId());
        b.setDriverName(event.driverName());
        b.setVehicle(event.vehicle());
        b.setStatus(BookingStatus.DRIVER_ASSIGNED);
        b = bookings.save(b);
        publisher.publish(b, RideEventType.DRIVER_ASSIGNED);
    }

    public BookingDto cancel(AuthUser user, Long id) {
        Booking b = load(id);
        if (!user.id().equals(b.getRiderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the rider can cancel this ride");
        }
        if (b.getStatus() != BookingStatus.REQUESTED && b.getStatus() != BookingStatus.DRIVER_ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A ride can't be cancelled once it has started");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b = bookings.save(b);
        publisher.publish(b, RideEventType.RIDE_CANCELLED);
        return BookingDto.from(b);
    }

    public BookingDto start(AuthUser driver, Long id) {
        Booking b = loadForDriver(driver, id);
        if (b.getStatus() != BookingStatus.DRIVER_ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride can't be started from status " + b.getStatus());
        }
        b.setStatus(BookingStatus.IN_PROGRESS);
        b = bookings.save(b);
        publisher.publish(b, RideEventType.RIDE_STARTED);
        return BookingDto.from(b);
    }

    public BookingDto complete(AuthUser driver, Long id) {
        Booking b = loadForDriver(driver, id);
        if (b.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a ride in progress can be completed");
        }
        b.setStatus(BookingStatus.COMPLETED);
        b = bookings.save(b);
        publisher.publish(b, RideEventType.RIDE_COMPLETED);
        return BookingDto.from(b);
    }

    public BookingDto rate(AuthUser rider, Long id, int rating) {
        Booking b = load(id);
        if (!rider.id().equals(b.getRiderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the rider can rate this ride");
        }
        if (b.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed rides can be rated");
        }
        b.setRating(rating);
        return BookingDto.from(bookings.save(b));
    }

    public BookingDto get(AuthUser user, Long id) {
        Booking b = load(id);
        if (!user.id().equals(b.getRiderId()) && !user.id().equals(b.getDriverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your ride");
        }
        return BookingDto.from(b);
    }

    public Optional<BookingDto> active(AuthUser user) {
        Optional<Booking> b = user.isDriver()
                ? bookings.findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(user.id(), BookingStatus.ACTIVE)
                : bookings.findFirstByRiderIdAndStatusInOrderByCreatedAtDesc(user.id(), BookingStatus.ACTIVE);
        return b.map(BookingDto::from);
    }

    public List<BookingDto> history(AuthUser user) {
        List<Booking> list = user.isDriver()
                ? bookings.findTop50ByDriverIdOrderByCreatedAtDesc(user.id())
                : bookings.findTop50ByRiderIdOrderByCreatedAtDesc(user.id());
        return list.stream().map(BookingDto::from).toList();
    }

    private Booking load(Long id) {
        return bookings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
    }

    private Booking loadForDriver(AuthUser driver, Long id) {
        Booking b = load(id);
        if (!driver.id().equals(b.getDriverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This ride is assigned to another driver");
        }
        return b;
    }
}
