package com.rideflow.driver.simulator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Lets simulated drivers press "Start ride" / "Complete ride" exactly like the driver app does. */
@Component
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(@Value("${app.booking-url}") String bookingUrl) {
        this.restClient = RestClient.builder().baseUrl(bookingUrl).build();
    }

    public void startRide(Long bookingId, String token) {
        post("/api/bookings/{id}/start", bookingId, token);
    }

    public void completeRide(Long bookingId, String token) {
        post("/api/bookings/{id}/complete", bookingId, token);
    }

    private void post(String path, Long bookingId, String token) {
        restClient.post()
                .uri(path, bookingId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }
}
