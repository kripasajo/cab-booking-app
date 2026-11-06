package com.rideflow.booking.client;

import com.rideflow.common.dto.FareEstimate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Synchronous call to pricing-service: the fare must be known before the ride is created. */
@Component
public class PricingClient {

    private final RestClient restClient;

    public PricingClient(@Value("${app.pricing-url}") String pricingUrl) {
        this.restClient = RestClient.builder().baseUrl(pricingUrl).build();
    }

    public FareEstimate estimate(double pickupLat, double pickupLng, double dropLat, double dropLng, String rideType) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/api/pricing/estimate")
                            .queryParam("pickupLat", pickupLat)
                            .queryParam("pickupLng", pickupLng)
                            .queryParam("dropLat", dropLat)
                            .queryParam("dropLng", dropLng)
                            .queryParam("rideType", rideType)
                            .build())
                    .retrieve()
                    .body(FareEstimate.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pricing service is unavailable, try again");
        }
    }
}
