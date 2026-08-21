package com.porterclone.matching.service;

import com.porterclone.rider.entity.AvailabilityStatus;
import com.porterclone.rider.entity.OnboardingStatus;
import com.porterclone.rider.entity.OnlineStatus;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import com.porterclone.rider.service.RiderAvailabilityService;
import com.porterclone.vehicle.entity.Vehicle;
import com.porterclone.vehicle.entity.VerificationStatus;
import com.porterclone.vehicle.repository.VehicleRepository;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maintains rider live locations in Redis using GEO commands, keyed per vehicle type
 * so matching queries only ever scan riders operating a compatible vehicle.
 *
 * Key pattern: "riders:location:{vehicleTypeId}" -> Redis GEO set of riderId -> (lng, lat)
 */
@Service
public class RiderLocationService {

    private static final String LOCATION_KEY_PREFIX = "riders:location:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RiderRepository riderRepository;
    private final VehicleRepository vehicleRepository;
    private final RiderAvailabilityService availabilityService;

    public RiderLocationService(RedisTemplate<String, String> redisTemplate,
                                 RiderRepository riderRepository,
                                 VehicleRepository vehicleRepository,
                                 RiderAvailabilityService availabilityService) {
        this.redisTemplate = redisTemplate;
        this.riderRepository = riderRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityService = availabilityService;
    }

    /**
     * Call on every location ping from the rider app (every 4-5s).
     * Vehicle type is NOT taken from the client — it's resolved server-side from
     * Rider.currentVehicleId (set via VehicleService.selectActiveVehicle()). This closes
     * a spoofing gap where a rider could otherwise claim to be driving any vehicle type
     * on each ping regardless of what they actually registered/got verified for.
     * Only adds the rider to the matchable geo set if they're actually eligible right now.
     */
    public void updateLocation(Long riderId, double lat, double lng) {
        availabilityService.refreshHeartbeat(riderId);

        Rider rider = riderRepository.findById(riderId).orElse(null);
        Vehicle activeVehicle = (rider != null && rider.getCurrentVehicleId() != null)
                ? vehicleRepository.findById(rider.getCurrentVehicleId()).orElse(null)
                : null;

        boolean eligibleForMatching = rider != null
                && activeVehicle != null
                && activeVehicle.getVerificationStatus() == VerificationStatus.VERIFIED
                && activeVehicle.isActive()
                && rider.getOnboardingStatus() == OnboardingStatus.ACTIVE
                && rider.getOnlineStatus() == OnlineStatus.ONLINE
                && rider.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE;

        // Always store their raw last-known position, for "track my rider" on active trips
        // regardless of matching eligibility.
        redisTemplate.opsForValue().set("rider:last_location:" + riderId, lat + "," + lng);

        if (!eligibleForMatching) {
            // If they have a known vehicle type, make sure they're pulled out of that geo set
            // even if they were added previously (covers on-trip / offline / unverified cases).
            if (activeVehicle != null) {
                redisTemplate.opsForGeo().remove(LOCATION_KEY_PREFIX + activeVehicle.getVehicleTypeId(), String.valueOf(riderId));
            }
            return;
        }

        String key = LOCATION_KEY_PREFIX + activeVehicle.getVehicleTypeId();
        redisTemplate.opsForGeo().add(key, new Point(lng, lat), String.valueOf(riderId));
    }

    public void removeFromGeoSet(Long riderId, Long vehicleTypeId) {
        redisTemplate.opsForGeo().remove(LOCATION_KEY_PREFIX + vehicleTypeId, String.valueOf(riderId));
    }

    public Optional<double[]> getLastKnownLocation(Long riderId) {
        String raw = redisTemplate.opsForValue().get("rider:last_location:" + riderId);
        if (raw == null) return Optional.empty();
        String[] parts = raw.split(",");
        return Optional.of(new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])});
    }

    /**
     * Finds riders within radiusKm of the pickup point, nearest first, for the given vehicle type.
     * Returns rider IDs only — caller is responsible for re-validating eligibility against MySQL
     * before dispatching (geo set can be briefly stale between a status change and the next ping).
     */
    public List<Long> findNearbyRiders(Long vehicleTypeId, double lat, double lng, double radiusKm, int limit) {
        String key = LOCATION_KEY_PREFIX + vehicleTypeId;

        Circle within = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(key, within, args);

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(r -> Long.valueOf(r.getContent().getName()))
                .collect(Collectors.toList());
    }
}
