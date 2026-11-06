package com.rideflow.driver.simulator;

import com.rideflow.common.security.AuthUser;
import com.rideflow.common.security.JwtUtil;
import com.rideflow.common.util.GeoUtils;
import com.rideflow.driver.model.DriverState;
import com.rideflow.driver.service.DriverService;
import com.rideflow.driver.store.DriverStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demo helper: a few simulated drivers ("bots") so the app works with just one browser tab.
 *
 * Bots go online around the city centre. When one is matched to a ride it drives to the
 * pickup, starts the ride, drives to the drop and completes it, sending a location update
 * every second, through the same code paths a real driver uses.
 *
 * Turn it off with SIMULATOR_ENABLED=false.
 */
@Component
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true", matchIfMissing = true)
public class BotDriverSimulator {

    private static final Logger log = LoggerFactory.getLogger(BotDriverSimulator.class);

    private static final String[] NAMES = {"Ravi", "Priya", "Arjun", "Meera", "Kiran", "Sana", "Vikas", "Divya"};
    private static final String[] CARS = {
            "Swift Dzire · KA05MN2231", "Hyundai Aura · KA03QP8812", "Toyota Etios · KA01ZX4410",
            "Maruti Ertiga · KA51TT0921", "Honda Amaze · KA02BC7765", "Tata Tigor EV · KA04EV1102",
            "Kia Carens · KA53KC3388", "WagonR · KA01WR5521"};

    private final DriverStore store;
    private final DriverService driverService;
    private final BookingClient bookingClient;
    private final JwtUtil jwtUtil;
    private final List<AuthUser> bots = new CopyOnWriteArrayList<>();

    private final int botCount;
    private final double centerLat;
    private final double centerLng;
    private final double stepKm;

    public BotDriverSimulator(DriverStore store, DriverService driverService, BookingClient bookingClient,
                              JwtUtil jwtUtil,
                              @Value("${app.simulator.bots:6}") int botCount,
                              @Value("${app.simulator.center-lat:12.9716}") double centerLat,
                              @Value("${app.simulator.center-lng:77.5946}") double centerLng,
                              @Value("${app.simulator.step-km:0.08}") double stepKm) {
        this.store = store;
        this.driverService = driverService;
        this.bookingClient = bookingClient;
        this.jwtUtil = jwtUtil;
        this.botCount = Math.min(botCount, NAMES.length);
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.stepKm = stepKm;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void spawnBots() {
        Random random = new Random();
        for (int i = 0; i < botCount; i++) {
            AuthUser bot = new AuthUser("bot-" + (i + 1), NAMES[i] + " (sim)", "DRIVER", CARS[i]);
            bots.add(bot);
            // scatter bots up to ~2 km around the centre
            double lat = centerLat + (random.nextDouble() - 0.5) * 0.036;
            double lng = centerLng + (random.nextDouble() - 0.5) * 0.036;
            store.saveOnline(bot.id(), bot.name(), bot.vehicle(), lat, lng);
        }
        log.info("Simulator: {} bot drivers online around {},{}", botCount, centerLat, centerLng);
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void tick() {
        for (AuthUser bot : bots) {
            try {
                step(bot);
            } catch (Exception e) {
                log.warn("Simulator step failed for {}: {}", bot.id(), e.getMessage());
            }
        }
    }

    private void step(AuthUser bot) {
        DriverState s = store.get(bot.id());
        if (s == null || !s.isBusy() || s.bookingId() == null || s.phase() == null) {
            return;
        }
        if ("TO_PICKUP".equals(s.phase())) {
            if (driveTowards(s, s.pickupLat(), s.pickupLng())) {
                bookingClient.startRide(s.bookingId(), jwtUtil.generateToken(bot));
                store.setPhase(bot.id(), "TO_DROP");
            }
        } else if ("TO_DROP".equals(s.phase())) {
            if (driveTowards(s, s.dropLat(), s.dropLng())) {
                store.setPhase(bot.id(), "COMPLETING");
                bookingClient.completeRide(s.bookingId(), jwtUtil.generateToken(bot));
            }
        }
    }

    /** Moves one step in a straight line. Returns true once the target is reached. */
    private boolean driveTowards(DriverState s, Double targetLat, Double targetLng) {
        if (targetLat == null || targetLng == null) {
            return false;
        }
        double remainingKm = GeoUtils.distanceKm(s.lat(), s.lng(), targetLat, targetLng);
        if (remainingKm < 0.01) {
            return true;
        }
        double fraction = Math.min(1.0, stepKm / remainingKm);
        double lat = s.lat() + (targetLat - s.lat()) * fraction;
        double lng = s.lng() + (targetLng - s.lng()) * fraction;
        driverService.updateLocation(s.driverId(), lat, lng);
        return fraction >= 1.0;
    }
}
