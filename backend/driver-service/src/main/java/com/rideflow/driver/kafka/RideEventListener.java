package com.rideflow.driver.kafka;

import com.rideflow.common.event.RideEvent;
import com.rideflow.common.kafka.Topics;
import com.rideflow.driver.service.DriverService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RideEventListener {

    private final DriverService driverService;

    public RideEventListener(DriverService driverService) {
        this.driverService = driverService;
    }

    @KafkaListener(topics = Topics.RIDE_EVENTS)
    public void onRideEvent(RideEvent event) {
        switch (event.type()) {
            case RIDE_REQUESTED -> driverService.matchRide(event);
            case RIDE_STARTED -> driverService.onRideStarted(event);
            case RIDE_COMPLETED, RIDE_CANCELLED, DRIVER_RELEASED -> driverService.releaseDriver(event);
            default -> {
                // DRIVER_ASSIGNED / NO_DRIVER_FOUND need nothing from us
            }
        }
    }
}
