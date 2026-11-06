package com.rideflow.pricing;

import com.rideflow.common.dto.FareEstimate;
import com.rideflow.pricing.service.PricingCalculator;
import com.rideflow.pricing.service.RideType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    @Test
    void noSurgeWhenDriversOutnumberRides() {
        assertThat(PricingCalculator.surgeMultiplier(0, 5)).isEqualTo(1.0);
        assertThat(PricingCalculator.surgeMultiplier(2, 2)).isEqualTo(1.0);
    }

    @Test
    void surgeGrowsWithDemand() {
        assertThat(PricingCalculator.surgeMultiplier(3, 2)).isEqualTo(1.3); // 4/3
        assertThat(PricingCalculator.surgeMultiplier(5, 1)).isEqualTo(2.5); // 6/2 = 3, capped
    }

    @Test
    void miniFareForFiveKmWithoutSurge() {
        // 5 km at 25 km/h = 12 min -> 40 + 5*10 + 12*1 = 102
        FareEstimate e = PricingCalculator.estimate(5.0, RideType.MINI, 4, 0);
        assertThat(e.durationMin()).isEqualTo(12);
        assertThat(e.fare()).isEqualTo(102);
        assertThat(e.surgeMultiplier()).isEqualTo(1.0);
    }

    @Test
    void surgeIsAppliedToFare() {
        FareEstimate e = PricingCalculator.estimate(5.0, RideType.MINI, 1, 2); // surge 1.5
        assertThat(e.surgeMultiplier()).isEqualTo(1.5);
        assertThat(e.fare()).isEqualTo(153);
    }

    @Test
    void unknownRideTypeFallsBackToMini() {
        assertThat(RideType.parse("rocket")).isEqualTo(RideType.MINI);
        assertThat(RideType.parse("suv")).isEqualTo(RideType.SUV);
    }
}
