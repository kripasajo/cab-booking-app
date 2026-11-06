package com.rideflow.common;

import com.rideflow.common.util.GeoUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    void samePointIsZeroKm() {
        assertThat(GeoUtils.distanceKm(12.97, 77.59, 12.97, 77.59)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertThat(GeoUtils.distanceKm(12.0, 77.0, 13.0, 77.0)).isCloseTo(111.2, within(0.5));
    }

    @Test
    void roundsToDecimals() {
        assertThat(GeoUtils.round(3.14159, 2)).isEqualTo(3.14);
    }
}
