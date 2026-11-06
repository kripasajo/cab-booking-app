package com.rideflow.driver.store;

import com.rideflow.common.util.GeoUtils;
import com.rideflow.common.util.RedisKeys;
import com.rideflow.driver.model.DriverState;
import com.rideflow.driver.model.NearbyDriver;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All Redis access for drivers.
 *
 * Two GEO sets (Redis sorted sets with geohash scores):
 *  - drivers:locations  every online driver (for showing on the map)
 *  - drivers:available  only free drivers (for matching and surge pricing)
 * Plus one hash per driver with status / name / vehicle / current ride.
 */
@Component
public class DriverStore {

    private static final String[] RIDE_FIELDS = {"bookingId", "phase", "pickupLat", "pickupLng", "dropLat", "dropLng"};

    private final StringRedisTemplate redis;

    public DriverStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public DriverState get(String driverId) {
        Map<Object, Object> hash = redis.opsForHash().entries(RedisKeys.driver(driverId));
        return hash.isEmpty() ? null : DriverState.fromHash(driverId, hash);
    }

    /** Driver becomes AVAILABLE at the given position (any previous ride data is cleared). */
    public void saveOnline(String driverId, String name, String vehicle, double lat, double lng) {
        String key = RedisKeys.driver(driverId);
        Map<String, String> fields = new HashMap<>();
        fields.put("name", name == null ? "Driver" : name);
        fields.put("vehicle", vehicle == null ? "" : vehicle);
        fields.put("status", DriverState.AVAILABLE);
        fields.put("lat", String.valueOf(lat));
        fields.put("lng", String.valueOf(lng));
        redis.opsForHash().delete(key, (Object[]) RIDE_FIELDS);
        redis.opsForHash().putAll(key, fields);
        Point point = new Point(lng, lat); // Redis GEO takes longitude first
        redis.opsForGeo().add(RedisKeys.DRIVERS_ALL, point, driverId);
        redis.opsForGeo().add(RedisKeys.DRIVERS_AVAILABLE, point, driverId);
    }

    public void setOffline(String driverId) {
        redis.opsForHash().put(RedisKeys.driver(driverId), "status", DriverState.OFFLINE);
        redis.opsForGeo().remove(RedisKeys.DRIVERS_ALL, driverId);
        redis.opsForGeo().remove(RedisKeys.DRIVERS_AVAILABLE, driverId);
    }

    public void updatePosition(String driverId, double lat, double lng, boolean available) {
        String key = RedisKeys.driver(driverId);
        redis.opsForHash().put(key, "lat", String.valueOf(lat));
        redis.opsForHash().put(key, "lng", String.valueOf(lng));
        Point point = new Point(lng, lat);
        redis.opsForGeo().add(RedisKeys.DRIVERS_ALL, point, driverId);
        if (available) {
            redis.opsForGeo().add(RedisKeys.DRIVERS_AVAILABLE, point, driverId);
        }
    }

    /** Reserve the driver for a ride: BUSY + removed from the available set so nobody else gets them. */
    public void markBusy(String driverId, long bookingId, double pickupLat, double pickupLng,
                         double dropLat, double dropLng) {
        Map<String, String> fields = new HashMap<>();
        fields.put("status", DriverState.BUSY);
        fields.put("bookingId", String.valueOf(bookingId));
        fields.put("phase", "TO_PICKUP");
        fields.put("pickupLat", String.valueOf(pickupLat));
        fields.put("pickupLng", String.valueOf(pickupLng));
        fields.put("dropLat", String.valueOf(dropLat));
        fields.put("dropLng", String.valueOf(dropLng));
        redis.opsForHash().putAll(RedisKeys.driver(driverId), fields);
        redis.opsForGeo().remove(RedisKeys.DRIVERS_AVAILABLE, driverId);
    }

    public void setPhase(String driverId, String phase) {
        redis.opsForHash().put(RedisKeys.driver(driverId), "phase", phase);
    }

    /** Ride finished or cancelled: driver is free again at their current position. */
    public void markAvailable(String driverId) {
        DriverState state = get(driverId);
        if (state == null) {
            return;
        }
        String key = RedisKeys.driver(driverId);
        redis.opsForHash().delete(key, (Object[]) RIDE_FIELDS);
        redis.opsForHash().put(key, "status", DriverState.AVAILABLE);
        redis.opsForGeo().add(RedisKeys.DRIVERS_AVAILABLE, new Point(state.lng(), state.lat()), driverId);
    }

    /**
     * GEOSEARCH drivers:available FROMLONLAT lng lat BYRADIUS r km ASC COUNT limit WITHDIST WITHCOORD
     * Redis answers this in well under a millisecond, even with thousands of drivers.
     */
    public List<NearbyDriver> findNearestAvailable(double lat, double lng, double radiusKm, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                RedisKeys.DRIVERS_AVAILABLE,
                GeoReference.fromCoordinate(new Point(lng, lat)),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .includeCoordinates()
                        .sortAscending()
                        .limit(limit));

        List<NearbyDriver> drivers = new ArrayList<>();
        if (results == null) {
            return drivers;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String id = result.getContent().getName();
            Point p = result.getContent().getPoint();
            List<Object> info = redis.opsForHash().multiGet(RedisKeys.driver(id), List.of("name", "vehicle"));
            drivers.add(new NearbyDriver(
                    id,
                    info.get(0) == null ? "Driver" : info.get(0).toString(),
                    info.get(1) == null ? "" : info.get(1).toString(),
                    p.getY(),
                    p.getX(),
                    GeoUtils.round(result.getDistance().getValue(), 2)));
        }
        return drivers;
    }
}
