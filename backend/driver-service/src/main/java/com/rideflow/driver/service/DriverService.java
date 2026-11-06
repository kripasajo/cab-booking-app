package com.rideflow.driver.service;

import com.rideflow.common.event.DriverAssignmentEvent;
import com.rideflow.common.event.DriverLocationEvent;
import com.rideflow.common.event.RideEvent;
import com.rideflow.common.kafka.Topics;
import com.rideflow.common.security.AuthUser;
import com.rideflow.driver.dto.NearbyResponse;
import com.rideflow.driver.model.DriverState;
import com.rideflow.driver.model.NearbyDriver;
import com.rideflow.driver.store.DriverStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    private final DriverStore store;
    private final KafkaTemplate<String, Object> kafka;
    private final double matchRadiusKm;

    public DriverService(DriverStore store, KafkaTemplate<String, Object> kafka,
                         @Value("${app.matching.radius-km:5}") double matchRadiusKm) {
        this.store = store;
        this.kafka = kafka;
        this.matchRadiusKm = matchRadiusKm;
    }

    public DriverState me(AuthUser user) {
        DriverState state = store.get(user.id());
        return state != null ? state : DriverState.offline(user.id(), user.name(), user.vehicle());
    }

    public DriverState goOnline(AuthUser user, double lat, double lng) {
        DriverState state = store.get(user.id());
        if (state != null && state.isBusy()) {
            return updateLocation(user.id(), lat, lng); // already on a ride, just move
        }
        store.saveOnline(user.id(), user.name(), user.vehicle(), lat, lng);
        log.info("Driver {} is online at {},{}", user.id(), lat, lng);
        return store.get(user.id());
    }

    public DriverState goOffline(String driverId) {
        DriverState state = store.get(driverId);
        if (state != null && state.isBusy()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finish your current ride before going offline");
        }
        store.setOffline(driverId);
        return store.get(driverId);
    }

    /** Save the new position in Redis GEO and stream it to Kafka (riders follow it live). */
    public DriverState updateLocation(String driverId, double lat, double lng) {
        DriverState state = store.get(driverId);
        if (state == null || state.isOffline()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Go online first");
        }
        store.updatePosition(driverId, lat, lng, state.isAvailable());
        kafka.send(Topics.DRIVER_LOCATIONS, driverId,
                new DriverLocationEvent(driverId, state.bookingId(), lat, lng, System.currentTimeMillis()));
        return state.withPosition(lat, lng);
    }

    public NearbyResponse nearby(double lat, double lng, double radiusKm) {
        long start = System.nanoTime();
        List<NearbyDriver> drivers = store.findNearestAvailable(lat, lng, radiusKm, 20);
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        return new NearbyResponse(drivers, Math.round(ms * 100) / 100.0);
    }

    public DriverState location(String driverId) {
        DriverState state = store.get(driverId);
        if (state == null || state.isOffline()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver is offline");
        }
        return state;
    }

    /**
     * Matching: find the closest AVAILABLE driver to the pickup point with Redis GEOSEARCH,
     * reserve them and publish the result. Kafka delivers ride events one at a time per
     * consumer, so two rides can never grab the same driver.
     */
    public void matchRide(RideEvent ride) {
        long start = System.nanoTime();
        List<NearbyDriver> nearest = store.findNearestAvailable(ride.pickupLat(), ride.pickupLng(), matchRadiusKm, 1);
        double ms = (System.nanoTime() - start) / 1_000_000.0;

        if (nearest.isEmpty()) {
            log.info("No driver within {} km for booking {}", matchRadiusKm, ride.bookingId());
            kafka.send(Topics.DRIVER_ASSIGNMENTS, String.valueOf(ride.bookingId()), new DriverAssignmentEvent(
                    ride.bookingId(), false, null, null, null, 0, System.currentTimeMillis()));
            return;
        }

        NearbyDriver driver = nearest.get(0);
        store.markBusy(driver.driverId(), ride.bookingId(), ride.pickupLat(), ride.pickupLng(),
                ride.dropLat(), ride.dropLng());
        log.info("Matched booking {} to driver {} ({} km away) in {} ms",
                ride.bookingId(), driver.driverId(), driver.distanceKm(), String.format("%.2f", ms));
        kafka.send(Topics.DRIVER_ASSIGNMENTS, String.valueOf(ride.bookingId()), new DriverAssignmentEvent(
                ride.bookingId(), true, driver.driverId(), driver.name(), driver.vehicle(),
                driver.distanceKm(), System.currentTimeMillis()));
    }

    public void onRideStarted(RideEvent ride) {
        DriverState state = ride.driverId() == null ? null : store.get(ride.driverId());
        if (state != null && ride.bookingId().equals(state.bookingId())) {
            store.setPhase(ride.driverId(), "TO_DROP");
        }
    }

    /** Ride completed, cancelled or released: put the driver back into the available pool. */
    public void releaseDriver(RideEvent ride) {
        DriverState state = ride.driverId() == null ? null : store.get(ride.driverId());
        if (state != null && state.isBusy() && ride.bookingId().equals(state.bookingId())) {
            store.markAvailable(ride.driverId());
            log.info("Driver {} is available again", ride.driverId());
        }
    }
}
