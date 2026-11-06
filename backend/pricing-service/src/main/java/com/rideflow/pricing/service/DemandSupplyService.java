package com.rideflow.pricing.service;

import com.rideflow.common.util.RedisKeys;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

/**
 * Measures supply and demand around a point with Redis GEO:
 *  - supply = free drivers in drivers:available (written by driver-service)
 *  - demand = active rides in rides:active (written by this service from Kafka ride events)
 */
@Service
public class DemandSupplyService {

    private final StringRedisTemplate redis;

    public DemandSupplyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public int availableDriversNear(double lat, double lng, double radiusKm) {
        return countWithin(RedisKeys.DRIVERS_AVAILABLE, lat, lng, radiusKm);
    }

    public int activeRidesNear(double lat, double lng, double radiusKm) {
        return countWithin(RedisKeys.RIDES_ACTIVE, lat, lng, radiusKm);
    }

    public void rideStarted(long bookingId, double pickupLat, double pickupLng) {
        redis.opsForGeo().add(RedisKeys.RIDES_ACTIVE, new Point(pickupLng, pickupLat), String.valueOf(bookingId));
    }

    public void rideEnded(long bookingId) {
        redis.opsForGeo().remove(RedisKeys.RIDES_ACTIVE, String.valueOf(bookingId));
    }

    private int countWithin(String key, double lat, double lng, double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(new Point(lng, lat)),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs());
        return results == null ? 0 : results.getContent().size();
    }
}
