package com.rideflow.user.service;

import com.rideflow.common.event.UserRegisteredEvent;
import com.rideflow.common.kafka.Topics;
import com.rideflow.common.security.AuthUser;
import com.rideflow.common.security.JwtUtil;
import com.rideflow.user.dto.AuthResponse;
import com.rideflow.user.dto.LoginRequest;
import com.rideflow.user.dto.RegisterRequest;
import com.rideflow.user.dto.UserDto;
import com.rideflow.user.model.Role;
import com.rideflow.user.model.User;
import com.rideflow.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafka;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, KafkaTemplate<String, Object> kafka) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.kafka = kafka;
    }

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        if (req.role() == Role.DRIVER) {
            boolean hasVehicle = req.vehicle() != null && !req.vehicle().isBlank();
            user.setVehicle(hasVehicle ? req.vehicle().trim() : "Hatchback");
        }
        user = users.save(user);
        log.info("Registered {} {} ({})", user.getRole(), user.getEmail(), user.getId());

        // Event-driven: notification-service picks this up and sends a welcome message.
        kafka.send(Topics.USER_EVENTS, String.valueOf(user.getId()), new UserRegisteredEvent(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name(), System.currentTimeMillis()));

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return toAuthResponse(user);
    }

    public UserDto getUser(Long id) {
        return users.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AuthResponse toAuthResponse(User user) {
        AuthUser principal = new AuthUser(
                String.valueOf(user.getId()), user.getName(), user.getRole().name(), user.getVehicle());
        return new AuthResponse(jwtUtil.generateToken(principal), UserDto.from(user));
    }
}
