package com.porterclone.matching.service;

import com.porterclone.common.exception.ApiException;
import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.rider.entity.AvailabilityStatus;
import com.porterclone.rider.entity.OnboardingStatus;
import com.porterclone.rider.entity.OnlineStatus;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import com.porterclone.rider.service.RiderAvailabilityService;
import com.porterclone.vehicle.entity.VerificationStatus;
import com.porterclone.vehicle.repository.VehicleRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Finds and assigns the nearest eligible rider to a delivery request.
 *
 * Concurrency note: two riders could theoretically try to accept the same trip in the same
 * instant (e.g. a broadcast-to-N-riders dispatch strategy). We guard trip assignment with a
 * short-lived Redisson distributed lock keyed per trip so only the first accept wins — the
 * second caller gets a clean "already assigned" error instead of silently double-booking.
 */
@Service
public class RiderMatchingService {

    private final RiderLocationService riderLocationService;
    private final RiderRepository riderRepository;
    private final VehicleRepository vehicleRepository;
    private final RiderAvailabilityService riderAvailabilityService;
    private final RedissonClient redissonClient;
    private final double searchRadiusKm;
    private final double maxRadiusKm;
    private final int candidatesPerRound;

    public RiderMatchingService(RiderLocationService riderLocationService,
                                RiderRepository riderRepository,
                                VehicleRepository vehicleRepository,
                                RiderAvailabilityService riderAvailabilityService,
                                RedissonClient redissonClient,
                                @Value("${app.matching.search-radius-km}") double searchRadiusKm,
                                @Value("${app.matching.max-radius-km}") double maxRadiusKm,
                                @Value("${app.matching.candidates-per-round}") int candidatesPerRound) {
        this.riderLocationService = riderLocationService;
        this.riderRepository = riderRepository;
        this.vehicleRepository = vehicleRepository;
        this.riderAvailabilityService = riderAvailabilityService;
        this.redissonClient = redissonClient;
        this.searchRadiusKm = searchRadiusKm;
        this.maxRadiusKm = maxRadiusKm;
        this.candidatesPerRound = candidatesPerRound;
    }

    /**
     * Returns nearest eligible rider IDs for a pickup point, re-validated against MySQL
     * (the Redis geo set can be briefly stale relative to a rider's true status).
     * Widens the search radius once if the initial radius finds nobody.
     */
    public List<Long> findEligibleCandidates(Long vehicleTypeId, double pickupLat, double pickupLng) {
        List<Long> candidates = riderLocationService.findNearbyRiders(
                vehicleTypeId, pickupLat, pickupLng, searchRadiusKm, candidatesPerRound);

        if (candidates.isEmpty()) {
            candidates = riderLocationService.findNearbyRiders(
                    vehicleTypeId, pickupLat, pickupLng, maxRadiusKm, candidatesPerRound);
        }

        return candidates.stream()
                .filter(this::isGenuinelyEligible)
                .toList();
    }

    private boolean isGenuinelyEligible(Long riderId) {
        Optional<Rider> riderOpt = riderRepository.findById(riderId);
        if (riderOpt.isEmpty()) return false;
        Rider rider = riderOpt.get();

        boolean statusOk = rider.getOnboardingStatus() == OnboardingStatus.ACTIVE
                && rider.getOnlineStatus() == OnlineStatus.ONLINE
                && rider.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE
                && rider.getCurrentVehicleId() != null;

        if (!statusOk) return false;

        // Re-check the vehicle itself at match time — closes the window where a rider's
        // vehicle gets un-verified by admin between their last location ping and this match.
        boolean vehicleOk = vehicleRepository.findById(rider.getCurrentVehicleId())
                .map(v -> v.getVerificationStatus() == VerificationStatus.VERIFIED && v.isActive())
                .orElse(false);

        return vehicleOk && riderAvailabilityService.isHeartbeatAlive(riderId);
    }

    /** Why an assignment attempt did or didn't succeed — lets the caller give an accurate error
     *  instead of collapsing every failure into a generic "already assigned" message. */
    public enum AssignmentOutcome {
        WON,                 // this caller is now the assigned rider
        LOCK_CONTENDED,      // couldn't get the lock in time — safe to retry
        ALREADY_ASSIGNED,    // trip.getRiderId() was already set by someone else
        RIDER_NOT_ELIGIBLE   // this rider specifically failed isGenuinelyEligible (heartbeat expired,
        // went offline/on-trip/on-break, vehicle no longer verified, etc.)
    }

    /**
     * Attempts to atomically assign a rider to a trip. Returns WON if THIS caller won the
     * assignment race; otherwise returns the specific reason it didn't, so the caller can
     * surface an accurate error instead of a blanket "already assigned".
     */
    public AssignmentOutcome tryAssignRider(DeliveryRequest trip, Long riderId) {
        String lockKey = "lock:trip-assignment:" + trip.getId();
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!acquired) {
                return AssignmentOutcome.LOCK_CONTENDED;
            }

            // Re-check under the lock: has this trip already been assigned by a concurrent request?
            if (trip.getRiderId() != null) {
                return AssignmentOutcome.ALREADY_ASSIGNED;
            }
            if (!isGenuinelyEligible(riderId)) {
                return AssignmentOutcome.RIDER_NOT_ELIGIBLE;
            }

            trip.setRiderId(riderId);
            riderAvailabilityService.markOnTrip(riderId);
            return AssignmentOutcome.WON;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.badRequest("ASSIGNMENT_INTERRUPTED", "Rider assignment was interrupted, please retry");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}