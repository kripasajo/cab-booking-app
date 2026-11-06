package com.rideflow.pricing.service;

import com.rideflow.common.dto.FareEstimate;
import com.rideflow.common.util.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private final DemandSupplyService demandSupply;
    private final double surgeRadiusKm;

    public PricingService(DemandSupplyService demandSupply,
                          @Value("${app.surge.radius-km:3}") double surgeRadiusKm) {
        this.demandSupply = demandSupply;
        this.surgeRadiusKm = surgeRadiusKm;
    }

    public FareEstimate estimate(double pickupLat, double pickupLng, double dropLat, double dropLng, String rideType) {
        double distanceKm = GeoUtils.distanceKm(pickupLat, pickupLng, dropLat, dropLng);
        int supply = demandSupply.availableDriversNear(pickupLat, pickupLng, surgeRadiusKm);
        int demand = demandSupply.activeRidesNear(pickupLat, pickupLng, surgeRadiusKm);
        return PricingCalculator.estimate(distanceKm, RideType.parse(rideType), supply, demand);
    }
}
