package com.rideflow.user.config;

import com.rideflow.user.model.Role;
import com.rideflow.user.model.User;
import com.rideflow.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates two demo accounts on startup so the app can be tried immediately. */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        create("Demo Rider", "rider@demo.com", Role.RIDER, null);
        create("Demo Driver", "driver@demo.com", Role.DRIVER, "White Swift Dzire · KA01AB1234");
    }

    private void create(String name, String email, Role role, String vehicle) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setVehicle(vehicle);
        users.save(user);
        log.info("Created demo account {} / {}", email, DEMO_PASSWORD);
    }
}
