package com.rideflow.pricing.controller;

import com.rideflow.common.dto.FareEstimate;
import com.rideflow.pricing.service.PricingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /** Public: lets the rider see prices before booking, and is called by booking-service. */
    @GetMapping("/estimate")
    public FareEstimate estimate(@RequestParam double pickupLat, @RequestParam double pickupLng,
                                 @RequestParam double dropLat, @RequestParam double dropLng,
                                 @RequestParam(defaultValue = "MINI") String rideType) {
        return pricingService.estimate(pickupLat, pickupLng, dropLat, dropLng, rideType);
    }
}
