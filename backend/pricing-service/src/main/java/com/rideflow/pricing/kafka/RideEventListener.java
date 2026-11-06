package com.rideflow.pricing.kafka;

import com.rideflow.common.event.RideEvent;
import com.rideflow.common.kafka.Topics;
import com.rideflow.pricing.service.DemandSupplyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Keeps the demand side of surge pricing up to date from the ride lifecycle stream. */
@Component
public class RideEventListener {

    private final DemandSupplyService demandSupply;

    public RideEventListener(DemandSupplyService demandSupply) {
        this.demandSupply = demandSupply;
    }

    @KafkaListener(topics = Topics.RIDE_EVENTS)
    public void onRideEvent(RideEvent event) {
        switch (event.type()) {
            case RIDE_REQUESTED -> demandSupply.rideStarted(event.bookingId(), event.pickupLat(), event.pickupLng());
            case RIDE_COMPLETED, RIDE_CANCELLED, NO_DRIVER_FOUND -> demandSupply.rideEnded(event.bookingId());
            default -> {
                // other events don't change demand
            }
        }
    }
}
