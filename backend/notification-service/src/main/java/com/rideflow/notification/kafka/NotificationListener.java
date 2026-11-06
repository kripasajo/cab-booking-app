package com.rideflow.notification.kafka;

import com.rideflow.common.event.DriverLocationEvent;
import com.rideflow.common.event.RideEvent;
import com.rideflow.common.event.RideEventType;
import com.rideflow.common.event.UserRegisteredEvent;
import com.rideflow.common.kafka.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges Kafka to the browser: every event consumed here is pushed to the
 * matching WebSocket topic so riders and drivers see changes instantly.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final SimpMessagingTemplate ws;

    public NotificationListener(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    @KafkaListener(topics = Topics.RIDE_EVENTS)
    public void onRideEvent(RideEvent event) {
        if (event.type() == RideEventType.DRIVER_RELEASED) {
            return; // internal bookkeeping only
        }
        ws.convertAndSend("/topic/rides/" + event.bookingId(), event);

        boolean driverNeedsToKnow = event.type() == RideEventType.DRIVER_ASSIGNED
                || event.type() == RideEventType.RIDE_CANCELLED;
        if (driverNeedsToKnow && event.driverId() != null) {
            ws.convertAndSend("/topic/drivers/" + event.driverId(), event);
        }
        log.info("Pushed {} for ride {} ({})", event.type(), event.bookingId(), describe(event));
    }

    @KafkaListener(topics = Topics.DRIVER_LOCATIONS)
    public void onDriverLocation(DriverLocationEvent event) {
        if (event.bookingId() != null) {
            ws.convertAndSend("/topic/rides/" + event.bookingId() + "/location", event);
        }
    }

    @KafkaListener(topics = Topics.USER_EVENTS)
    public void onUserRegistered(UserRegisteredEvent event) {
        // A real system would send an email / SMS here.
        log.info("Welcome message sent to {} <{}> ({})", event.name(), event.email(), event.role());
    }

    private String describe(RideEvent e) {
        return switch (e.type()) {
            case RIDE_REQUESTED -> "rider " + e.riderName() + " is looking for a cab";
            case DRIVER_ASSIGNED -> e.driverName() + " is on the way";
            case NO_DRIVER_FOUND -> "no drivers nearby";
            case RIDE_STARTED -> "trip started";
            case RIDE_COMPLETED -> "trip finished, fare Rs " + e.fare();
            case RIDE_CANCELLED -> "ride cancelled";
            case DRIVER_RELEASED -> "driver released";
        };
    }
}
