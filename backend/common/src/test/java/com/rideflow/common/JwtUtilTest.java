package com.rideflow.common;

import com.rideflow.common.security.AuthUser;
import com.rideflow.common.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-secret-that-is-at-least-32-bytes-long!!", 60_000);

    @Test
    void tokenRoundTripKeepsAllClaims() {
        AuthUser user = new AuthUser("42", "Asha", "DRIVER", "Swift Dzire KA01AB1234");
        AuthUser parsed = jwtUtil.parseToken(jwtUtil.generateToken(user));
        assertThat(parsed).isEqualTo(user);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil other = new JwtUtil("a-completely-different-secret-of-32-bytes!!", 60_000);
        String token = other.generateToken(new AuthUser("1", "Eve", "RIDER", null));
        assertThatThrownBy(() -> jwtUtil.parseToken(token)).isInstanceOf(JwtException.class);
    }
}
