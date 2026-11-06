package com.rideflow.booking.kafka;

import com.rideflow.booking.service.BookingService;
import com.rideflow.common.event.DriverAssignmentEvent;
import com.rideflow.common.kafka.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Receives matching results from driver-service. */
@Component
public class DriverAssignmentListener {

    private final BookingService bookingService;

    public DriverAssignmentListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @KafkaListener(topics = Topics.DRIVER_ASSIGNMENTS)
    public void onAssignment(DriverAssignmentEvent event) {
        bookingService.handleAssignment(event);
    }
}
