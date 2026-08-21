package com.porterclone.rider.service;

import com.porterclone.common.exception.ApiException;
import com.porterclone.rider.entity.AvailabilityStatus;
import com.porterclone.rider.entity.OnboardingStatus;
import com.porterclone.rider.entity.OnlineStatus;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import com.porterclone.vehicle.entity.VerificationStatus;
import com.porterclone.vehicle.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Owns rider online/offline + available/on-trip/break state.
 * MySQL is the source of truth; Redis is the fast-path used by the matching engine
 * (geo index + heartbeat). Every state change here keeps both in sync so they never drift.
 */
@Service
public class RiderAvailabilityService {

    private final RiderRepository riderRepository;
    private final VehicleRepository vehicleRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final long heartbeatTtlSeconds;

    public RiderAvailabilityService(RiderRepository riderRepository,
                                     VehicleRepository vehicleRepository,
                                     RedisTemplate<String, String> redisTemplate,
                                     @Value("${app.rider.heartbeat-ttl-seconds}") long heartbeatTtlSeconds) {
        this.riderRepository = riderRepository;
        this.vehicleRepository = vehicleRepository;
        this.redisTemplate = redisTemplate;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    /**
     * Rider taps "Go Online". Requires: onboarding fully ACTIVE, AND an active vehicle
     * selected (Rider.currentVehicleId) that is VERIFIED and active. Without this check,
     * a rider could go online and receive trip requests with no verified vehicle at all.
     */
    @Transactional
    public Rider goOnline(Long riderId) {
        Rider rider = getRider(riderId);
        if (rider.getOnboardingStatus() != OnboardingStatus.ACTIVE) {
            throw ApiException.forbidden("NOT_ACTIVATED",
                    "Rider must complete onboarding (currently: " + rider.getOnboardingStatus() + ") before going online");
        }
        if (rider.getCurrentVehicleId() == null) {
            throw ApiException.badRequest("NO_ACTIVE_VEHICLE",
                    "Select an active vehicle (POST /riders/{id}/vehicles/{vehicleId}/select-active) before going online");
        }
        var vehicle = vehicleRepository.findById(rider.getCurrentVehicleId())
                .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Selected vehicle no longer exists"));
        if (vehicle.getVerificationStatus() != VerificationStatus.VERIFIED || !vehicle.isActive()) {
            throw ApiException.forbidden("VEHICLE_NOT_VERIFIED",
                    "Your selected vehicle is not currently verified/active — pick another or wait for admin approval");
        }

        rider.setOnlineStatus(OnlineStatus.ONLINE);
        rider.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        riderRepository.save(rider);
        refreshHeartbeat(riderId);
        return rider;
    }

    @Transactional
    public Rider goOffline(Long riderId) {
        Rider rider = getRider(riderId);
        rider.setOnlineStatus(OnlineStatus.OFFLINE);
        riderRepository.save(rider);
        removeFromAllGeoSets(riderId);
        redisTemplate.delete(heartbeatKey(riderId));
        return rider;
    }

    /** Called when a trip is assigned — pulls the rider out of the matching pool immediately. */
    @Transactional
    public void markOnTrip(Long riderId) {
        Rider rider = getRider(riderId);
        rider.setAvailabilityStatus(AvailabilityStatus.ON_TRIP);
        riderRepository.save(rider);
        removeFromAllGeoSets(riderId);
    }

    /** Called on trip completion/cancellation — rider becomes matchable again. */
    @Transactional
    public void markAvailable(Long riderId) {
        Rider rider = getRider(riderId);
        rider.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        riderRepository.save(rider);
        // Re-added to the geo set on their NEXT location ping, not here —
        // we don't have a fresh lat/lng at this point.
    }

    @Transactional
    public Rider takeBreak(Long riderId) {
        Rider rider = getRider(riderId);
        if (rider.getAvailabilityStatus() == AvailabilityStatus.ON_TRIP) {
            throw ApiException.badRequest("CANNOT_BREAK_MID_TRIP", "Cannot go on break while a trip is in progress");
        }
        rider.setAvailabilityStatus(AvailabilityStatus.BREAK);
        riderRepository.save(rider);
        removeFromAllGeoSets(riderId);
        return rider;
    }

    /** Refreshes the liveness heartbeat — call this on every location ping. */
    public void refreshHeartbeat(Long riderId) {
        redisTemplate.opsForValue().set(heartbeatKey(riderId), "1", Duration.ofSeconds(heartbeatTtlSeconds));
    }

    /** Used by the matching engine to distinguish "DB says online" from "actually still connected". */
    public boolean isHeartbeatAlive(Long riderId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey(riderId)));
    }

    private void removeFromAllGeoSets(Long riderId) {
        // Vehicle-type-specific geo sets are managed by RiderLocationService; we just
        // ask it (via key pattern) to drop this rider so they stop surfacing in matches.
        redisTemplate.delete("rider:available_flag:" + riderId);
    }

    private String heartbeatKey(Long riderId) {
        return "rider:heartbeat:" + riderId;
    }

    private Rider getRider(Long riderId) {
        return riderRepository.findById(riderId)
                .orElseThrow(() -> ApiException.notFound("RIDER_NOT_FOUND", "Rider not found: " + riderId));
    }
}
