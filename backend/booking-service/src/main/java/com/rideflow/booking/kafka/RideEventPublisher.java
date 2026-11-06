package com.rideflow.booking.kafka;

import com.rideflow.booking.model.Booking;
import com.rideflow.common.event.RideEvent;
import com.rideflow.common.event.RideEventType;
import com.rideflow.common.kafka.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a ride event every time a booking changes.
 * The booking id is the Kafka key, so all events of one ride land on the same
 * partition and are consumed in order.
 */
@Component
public class RideEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RideEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public RideEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publish(Booking b, RideEventType type) {
        publish(b, type, b.getDriverId());
    }

    public void publish(Booking b, RideEventType type, String driverId) {
        RideEvent event = new RideEvent(type, b.getId(), b.getStatus().name(),
                b.getRiderId(), b.getRiderName(), driverId, b.getDriverName(), b.getVehicle(),
                b.getPickupLat(), b.getPickupLng(), b.getDropLat(), b.getDropLng(),
                b.getDistanceKm(), b.getFare(), b.getSurgeMultiplier(), b.getRideType(),
                System.currentTimeMillis());
        kafka.send(Topics.RIDE_EVENTS, String.valueOf(b.getId()), event);
        log.info("Published {} for booking {}", type, b.getId());
    }
}
