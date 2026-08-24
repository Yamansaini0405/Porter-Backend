package com.porterclone.delivery.service;

import com.porterclone.commission.service.CommissionService;
import com.porterclone.common.GeoUtils;
import com.porterclone.common.exception.ApiException;
import com.porterclone.common.exception.InvalidStateTransitionException;
import com.porterclone.delivery.dto.CreateDeliveryRequest;
import com.porterclone.delivery.dto.FareEstimateResponse;
import com.porterclone.delivery.dto.LocationOnlyRequest;
import com.porterclone.delivery.dto.VehicleFareOption;
import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.delivery.entity.TripStatus;
import com.porterclone.delivery.entity.TripStatusHistory;
import com.porterclone.delivery.repository.DeliveryRequestRepository;
import com.porterclone.delivery.repository.TripStatusHistoryRepository;
import com.porterclone.matching.service.RiderLocationService;
import com.porterclone.matching.service.RiderMatchingService;
import com.porterclone.pricing.dto.FareBreakdown;
import com.porterclone.pricing.dto.FareCalculationRequest;
import com.porterclone.pricing.entity.FareDetails;
import com.porterclone.pricing.repository.FareDetailsRepository;
import com.porterclone.pricing.service.FareCalculationService;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import com.porterclone.rider.service.RiderAvailabilityService;
import com.porterclone.vehicle.entity.Vehicle;
import com.porterclone.vehicle.entity.VehicleType;
import com.porterclone.vehicle.repository.VehicleRepository;
import com.porterclone.vehicle.repository.VehicleTypeRepository;
import com.porterclone.zone.entity.Zone;
import com.porterclone.zone.service.ZoneService;
import com.porterclone.websocket.RequestBroadcastService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Owns the delivery/trip lifecycle end to end: creation, fare estimate, dispatch,
 * rider-side status updates, and final fare + commission settlement on completion.
 * Like RiderOnboardingService, all status changes flow through transition() so an
 * illegal jump (e.g. REQUESTED straight to COMPLETED) is impossible.
 */
@Service
public class DeliveryService {

    private static final Map<TripStatus, Set<TripStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TripStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(TripStatus.REQUESTED, EnumSet.of(TripStatus.SEARCHING_RIDER, TripStatus.CANCELLED_BY_CUSTOMER));
        ALLOWED_TRANSITIONS.put(TripStatus.SEARCHING_RIDER, EnumSet.of(TripStatus.RIDER_ASSIGNED, TripStatus.NO_RIDER_FOUND, TripStatus.CANCELLED_BY_CUSTOMER));
        ALLOWED_TRANSITIONS.put(TripStatus.RIDER_ASSIGNED, EnumSet.of(TripStatus.RIDER_ARRIVED, TripStatus.CANCELLED_BY_CUSTOMER, TripStatus.CANCELLED_BY_RIDER));
        ALLOWED_TRANSITIONS.put(TripStatus.RIDER_ARRIVED, EnumSet.of(TripStatus.WAITING_AT_PICKUP, TripStatus.IN_TRANSIT, TripStatus.CANCELLED_BY_CUSTOMER));
        ALLOWED_TRANSITIONS.put(TripStatus.WAITING_AT_PICKUP, EnumSet.of(TripStatus.IN_TRANSIT, TripStatus.CANCELLED_BY_CUSTOMER));
        ALLOWED_TRANSITIONS.put(TripStatus.IN_TRANSIT, EnumSet.of(TripStatus.ARRIVED_AT_DROP));
        ALLOWED_TRANSITIONS.put(TripStatus.ARRIVED_AT_DROP, EnumSet.of(TripStatus.COMPLETED));
        // Terminal states: COMPLETED, CANCELLED_BY_CUSTOMER, CANCELLED_BY_RIDER, NO_RIDER_FOUND, EXPIRED — no outgoing transitions.
    }

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final TripStatusHistoryRepository historyRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final RiderRepository riderRepository;
    private final FareDetailsRepository fareDetailsRepository;
    private final FareCalculationService fareCalculationService;
    private final CommissionService commissionService;
    private final RiderMatchingService riderMatchingService;
    private final RiderLocationService riderLocationService;
    private final RiderAvailabilityService riderAvailabilityService;
    private final RequestBroadcastService requestBroadcastService;
    private final ZoneService zoneService;
    private final double maxRadiusKm;
    private final SecureRandom random = new SecureRandom();

    public DeliveryService(DeliveryRequestRepository deliveryRequestRepository,
                           TripStatusHistoryRepository historyRepository,
                           VehicleTypeRepository vehicleTypeRepository,
                           VehicleRepository vehicleRepository,
                           RiderRepository riderRepository,
                           FareDetailsRepository fareDetailsRepository,
                           FareCalculationService fareCalculationService,
                           CommissionService commissionService,
                           RiderMatchingService riderMatchingService,
                           RiderLocationService riderLocationService,
                           RiderAvailabilityService riderAvailabilityService,
                           RequestBroadcastService requestBroadcastService,
                           ZoneService zoneService,
                           @Value("${app.matching.max-radius-km}") double maxRadiusKm) {
        this.deliveryRequestRepository = deliveryRequestRepository;
        this.historyRepository = historyRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.riderRepository = riderRepository;
        this.fareDetailsRepository = fareDetailsRepository;
        this.fareCalculationService = fareCalculationService;
        this.commissionService = commissionService;
        this.riderMatchingService = riderMatchingService;
        this.riderLocationService = riderLocationService;
        this.riderAvailabilityService = riderAvailabilityService;
        this.requestBroadcastService = requestBroadcastService;
        this.zoneService = zoneService;
        this.maxRadiusKm = maxRadiusKm;
    }

    /**
     * Step 1 of the real customer flow: pickup/drop only, no vehicle chosen yet.
     * Returns a fare estimate for EVERY active vehicle type so the customer can compare
     * side-by-side and pick one — this is what "show me options" screens call.
     */
    public List<VehicleFareOption> estimateFareForAllVehicleTypes(LocationOnlyRequest req) {
        BigDecimal distanceKm = GeoUtils.haversineDistanceKm(
                req.pickupLat().doubleValue(), req.pickupLng().doubleValue(),
                req.dropLat().doubleValue(), req.dropLng().doubleValue());
        BigDecimal durationMin = GeoUtils.estimateDurationMin(distanceKm);

        return vehicleTypeRepository.findByActiveTrue().stream()
                .map(vehicleType -> {
                    FareBreakdown breakdown = fareCalculationService.calculate(new FareCalculationRequest(
                            vehicleType.getBaseFare(), vehicleType.getPerKmRate(), vehicleType.getPerMinRate(),
                            vehicleType.getFreeWaitMinutes(), vehicleType.getWaitChargePerMin(),
                            distanceKm, durationMin,
                            null, null,
                            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(5)
                    ));
                    return new VehicleFareOption(
                            vehicleType.getId(), vehicleType.getName(), vehicleType.getCapacityKg(),
                            distanceKm, durationMin, breakdown.grossFare());
                })
                .toList();
    }

    public DeliveryRequest getDeliveryDetailsById(Long deliveryId) {
        return deliveryRequestRepository.findById(deliveryId)
                .orElseThrow(() -> ApiException.notFound("DELIVERY_NOT_FOUND", "Delivery request not found: " + deliveryId));
    }


    /** Step 2: customer has now picked one vehicle type — same distance/duration, single fare. */
    public FareEstimateResponse estimateFare(CreateDeliveryRequest req) {
        VehicleType vehicleType = getVehicleType(req.vehicleTypeId());

        BigDecimal distanceKm = GeoUtils.haversineDistanceKm(
                req.pickupLat().doubleValue(), req.pickupLng().doubleValue(),
                req.dropLat().doubleValue(), req.dropLng().doubleValue());
        BigDecimal durationMin = GeoUtils.estimateDurationMin(distanceKm);

        FareBreakdown breakdown = fareCalculationService.calculate(new FareCalculationRequest(
                vehicleType.getBaseFare(), vehicleType.getPerKmRate(), vehicleType.getPerMinRate(),
                vehicleType.getFreeWaitMinutes(), vehicleType.getWaitChargePerMin(),
                distanceKm, durationMin,
                null, null,          // no wait time yet at estimate stage
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(5) // 5% tax placeholder
        ));

        return new FareEstimateResponse(distanceKm, durationMin, breakdown.grossFare());
    }

    /** Step: customer confirms and creates the actual delivery request. */
    @Transactional
    public DeliveryRequest createRequest(Long customerId, CreateDeliveryRequest req) {
        VehicleType vehicleType = getVehicleType(req.vehicleTypeId());

        Zone pickupZone = zoneService.resolveServiceableZone(req.pickupLat(), req.pickupLng())
                .orElseThrow(() -> ApiException.badRequest(
                        "PICKUP_LOCATION_NOT_SERVICEABLE",
                        "Pickup location is not serviceable"));
        Zone dropZone = zoneService.resolveServiceableZone(req.dropLat(), req.dropLng())
                .orElseThrow(() -> ApiException.badRequest(
                        "DROP_LOCATION_NOT_SERVICEABLE",
                        "Drop location is not serviceable"));

        FareEstimateResponse estimate = estimateFare(req);

        DeliveryRequest trip = new DeliveryRequest();
        trip.setCustomerId(customerId);
        trip.setVehicleTypeId(vehicleType.getId());
        trip.setPickupAddress(req.pickupAddress());
        trip.setPickupZoneId(pickupZone.getId());
        trip.setPickupLat(req.pickupLat());
        trip.setPickupLng(req.pickupLng());
        trip.setDropAddress(req.dropAddress());
        trip.setDropZoneId(dropZone.getId());
        trip.setDropLat(req.dropLat());
        trip.setDropLng(req.dropLng());
        trip.setEstimatedDistanceKm(estimate.estimatedDistanceKm());
        trip.setEstimatedFare(estimate.estimatedFare());
        trip.setOtpCode(generateOtp());
        trip.setStatus(TripStatus.REQUESTED);
        trip = deliveryRequestRepository.save(trip);

        recordHistory(trip.getId(), TripStatus.REQUESTED, req.pickupLat(), req.pickupLng());
        return trip;
    }

    /**
     * Broadcasts the request to EVERY eligible nearby rider at once — first to call
     * acceptTrip() wins. This replaces the old sequential "try nearest, then next"
     * approach: instead of Claude picking one rider on the customer's behalf, all
     * eligible riders see it simultaneously and self-select by accepting.
     */
    @Transactional
    public DeliveryRequest dispatch(Long tripId) {
        DeliveryRequest trip = getTrip(tripId);
        transition(trip, TripStatus.SEARCHING_RIDER);

        List<Long> candidates = riderMatchingService.findEligibleCandidates(
                trip.getVehicleTypeId(), trip.getPickupLat().doubleValue(), trip.getPickupLng().doubleValue());

        // IMPORTANT: no candidates *right now* does NOT mean NO_RIDER_FOUND. A rider can
        // come online, finish another trip, or drift into range at any point during the
        // broadcast window. Failing the trip here short-circuits the configured
        // app.matching.broadcast-timeout-seconds wait entirely — DispatchTimeoutScheduler
        // is the only thing that should ever move a trip out of SEARCHING_RIDER on a timeout.
        // We just skip the broadcast (nobody to push to yet); the trip stays discoverable via
        // GET /deliveries/riders/{riderId}/pending for anyone who becomes eligible in the meantime.
        if (!candidates.isEmpty()) {
            RequestBroadcastService.NewRequestNotification notification = new RequestBroadcastService.NewRequestNotification(
                    trip.getId(), trip.getPickupAddress(), trip.getPickupLat(), trip.getPickupLng(),
                    trip.getDropAddress(), trip.getEstimatedFare(), trip.getVehicleTypeId(), trip.getRequestedAt());

            for (Long riderId : candidates) {
                requestBroadcastService.broadcastNewRequest(riderId, notification);
            }
        }

        // Trip stays in SEARCHING_RIDER with riderId still null — nobody's assigned yet.
        // Riders either react to the push above or poll GET /deliveries/riders/{riderId}/pending,
        // then call POST /{tripId}/riders/{riderId}/accept. DispatchTimeoutScheduler cleans up
        // if nobody accepts within the configured window.
        return trip;
    }

    /**
     * A rider taps "Accept" on a broadcasted request. Whoever's accept call wins the
     * Redisson lock inside tryAssignRider() gets the trip; everyone else gets a clean
     * "already taken" error — this is the actual first-accept-wins guarantee, not just
     * a UI convention, since two riders tapping accept in the same instant is exactly
     * the race this lock exists for.
     */
    @Transactional
    public DeliveryRequest acceptTrip(Long tripId, Long riderId) {
        DeliveryRequest trip = getTrip(tripId);

        if (trip.getStatus() != TripStatus.SEARCHING_RIDER) {
            throw ApiException.conflict("TRIP_NOT_AVAILABLE",
                    "This request is no longer available to accept (current status: " + trip.getStatus() + ")");
        }

        RiderMatchingService.AssignmentOutcome outcome = riderMatchingService.tryAssignRider(trip, riderId);
        switch (outcome) {
            case WON -> { /* fall through to assignment below */ }
            case ALREADY_ASSIGNED -> throw ApiException.conflict(
                    "TRIP_ALREADY_ASSIGNED", "Another rider already accepted this request");
            case LOCK_CONTENDED -> throw ApiException.conflict(
                    "TRIP_ASSIGNMENT_BUSY", "Another accept is being processed for this trip right now — please retry");
            case RIDER_NOT_ELIGIBLE -> throw ApiException.conflict(
                    "RIDER_NOT_ELIGIBLE", "You are no longer eligible to accept this trip right now — "
                            + "check that you're online, available, your vehicle is verified, and you've pinged your location recently");
        }

        trip.setAssignedAt(LocalDateTime.now());
        transition(trip, TripStatus.RIDER_ASSIGNED);
        requestBroadcastService.broadcastTripAssigned(trip.getId(), riderId);

        // Tell every other rider who was shown this request that it's gone, so their
        // app can pull it off screen immediately instead of leaving a stale "Accept" button.
        List<Long> otherCandidates = riderMatchingService.findEligibleCandidates(
                trip.getVehicleTypeId(), trip.getPickupLat().doubleValue(), trip.getPickupLng().doubleValue());
        for (Long candidateId : otherCandidates) {
            if (!candidateId.equals(riderId)) {
                requestBroadcastService.broadcastRequestClosed(candidateId, trip.getId(), "TAKEN");
            }
        }

        return trip;
    }

    /** Called by DispatchTimeoutScheduler when a broadcasted request goes unaccepted too long. */
    @Transactional
    public void expireSearch(Long tripId) {
        DeliveryRequest trip = getTrip(tripId);
        if (trip.getStatus() != TripStatus.SEARCHING_RIDER) {
            return; // already resolved (accepted/cancelled) by the time the sweep got to it
        }
        transition(trip, TripStatus.NO_RIDER_FOUND);

        List<Long> candidates = riderMatchingService.findEligibleCandidates(
                trip.getVehicleTypeId(), trip.getPickupLat().doubleValue(), trip.getPickupLng().doubleValue());
        for (Long candidateId : candidates) {
            requestBroadcastService.broadcastRequestClosed(candidateId, trip.getId(), "EXPIRED");
        }
    }

    /**
     * Backs the rider app's polling fallback (in case the WebSocket push was missed —
     * connection drop, app was backgrounded, etc). Same eligibility radius as the broadcast.
     */
    public List<DeliveryRequest> findPendingTripsForRider(Long riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> ApiException.notFound("RIDER_NOT_FOUND", "Rider not found"));
        if (rider.getCurrentVehicleId() == null) {
            return List.of();
        }
        Vehicle vehicle = vehicleRepository.findById(rider.getCurrentVehicleId()).orElse(null);
        if (vehicle == null) {
            return List.of();
        }
        Optional<double[]> lastLocation = riderLocationService.getLastKnownLocation(riderId);
        if (lastLocation.isEmpty()) {
            return List.of();
        }
        double riderLat = lastLocation.get()[0];
        double riderLng = lastLocation.get()[1];

        return deliveryRequestRepository.findByStatusAndVehicleTypeId(TripStatus.SEARCHING_RIDER, vehicle.getVehicleTypeId())
                .stream()
                .filter(t -> GeoUtils.haversineDistanceKm(
                                riderLat, riderLng, t.getPickupLat().doubleValue(), t.getPickupLng().doubleValue())
                        .doubleValue() <= maxRadiusKm)
                .toList();
    }

    @Transactional
    public DeliveryRequest markRiderArrived(Long tripId, Long riderId, BigDecimal lat, BigDecimal lng) {
        DeliveryRequest trip = getTripOwnedByRider(tripId, riderId);
        trip.setRiderArrivedAt(LocalDateTime.now());
        transition(trip, TripStatus.RIDER_ARRIVED, lat, lng);
        return trip;
    }

    /** Rider verifies pickup OTP with the customer and starts the trip — wait clock stops here. */
    @Transactional
    public DeliveryRequest startTrip(Long tripId, Long riderId, String otpCode) {
        DeliveryRequest trip = getTripOwnedByRider(tripId, riderId);
        if (!trip.getOtpCode().equals(otpCode)) {
            throw ApiException.badRequest("INVALID_OTP", "Pickup OTP does not match");
        }
        trip.setTripStartedAt(LocalDateTime.now());
        transition(trip, TripStatus.IN_TRANSIT);
        return trip;
    }

    @Transactional
    public DeliveryRequest markArrivedAtDrop(Long tripId, Long riderId, BigDecimal lat, BigDecimal lng) {
        DeliveryRequest trip = getTripOwnedByRider(tripId, riderId);
        transition(trip, TripStatus.ARRIVED_AT_DROP, lat, lng);
        return trip;
    }

    /** Final step: computes actual fare (incl. waiting charge), applies commission, credits rider. */
    @Transactional
    public DeliveryRequest completeTrip(Long tripId, Long riderId) {
        DeliveryRequest trip = getTripOwnedByRider(tripId, riderId);
        VehicleType vehicleType = getVehicleType(trip.getVehicleTypeId());

        BigDecimal actualDistanceKm = GeoUtils.haversineDistanceKm(
                trip.getPickupLat().doubleValue(), trip.getPickupLng().doubleValue(),
                trip.getDropLat().doubleValue(), trip.getDropLng().doubleValue());
        BigDecimal actualDurationMin = GeoUtils.estimateDurationMin(actualDistanceKm);

        FareBreakdown breakdown = fareCalculationService.calculate(new FareCalculationRequest(
                vehicleType.getBaseFare(), vehicleType.getPerKmRate(), vehicleType.getPerMinRate(),
                vehicleType.getFreeWaitMinutes(), vehicleType.getWaitChargePerMin(),
                actualDistanceKm, actualDurationMin,
                trip.getRiderArrivedAt(), trip.getTripStartedAt(),
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(5)
        ));

        CommissionService.CommissionResult commission = commissionService.computeCommission(
                trip.getVehicleTypeId(), breakdown.grossFare());

        FareDetails fareDetails = new FareDetails();
        fareDetails.setTripId(trip.getId());
        fareDetails.setBaseFare(breakdown.baseFare());
        fareDetails.setDistanceFare(breakdown.distanceFare());
        fareDetails.setTimeFare(breakdown.timeFare());
        fareDetails.setWaitingCharge(breakdown.waitingCharge());
        fareDetails.setSurgeAmount(breakdown.surgeAmount());
        fareDetails.setTollCharge(breakdown.tollCharge());
        fareDetails.setDiscountAmount(breakdown.discountAmount());
        fareDetails.setTaxAmount(breakdown.taxAmount());
        fareDetails.setGrossFare(breakdown.grossFare());
        fareDetails.setCommissionAmount(commission.commissionAmount());
        fareDetails.setCommissionPercentage(commission.commissionPercentage());
        fareDetails.setRiderNetEarning(commission.riderNetEarning());
        fareDetailsRepository.save(fareDetails);

        commissionService.postTripEarning(riderId, trip.getId(), commission.riderNetEarning());

        trip.setActualDistanceKm(actualDistanceKm);
        trip.setFinalFare(breakdown.grossFare());
        trip.setTripCompletedAt(LocalDateTime.now());
        transition(trip, TripStatus.COMPLETED);

        riderAvailabilityService.markAvailable(riderId);

        return trip;
    }

    @Transactional
    public DeliveryRequest cancel(Long tripId, String cancelledBy, String reason) {
        DeliveryRequest trip = getTrip(tripId);
        TripStatus target = "CUSTOMER".equals(cancelledBy) ? TripStatus.CANCELLED_BY_CUSTOMER : TripStatus.CANCELLED_BY_RIDER;
        transition(trip, target);
        trip.setCancelledAt(LocalDateTime.now());
        trip.setCancelledBy(cancelledBy);
        trip.setCancellationReason(reason);

        if (trip.getRiderId() != null) {
            riderAvailabilityService.markAvailable(trip.getRiderId());
        }
        return trip;
    }

    private static final java.util.Set<TripStatus> ACTIVE_RIDER_STATUSES = java.util.Set.of(
            TripStatus.RIDER_ASSIGNED, TripStatus.RIDER_ARRIVED, TripStatus.WAITING_AT_PICKUP,
            TripStatus.IN_TRANSIT, TripStatus.ARRIVED_AT_DROP);

    /**
     * What "does this rider have a request right now" actually means: their most recent
     * trip that's in an active (not yet completed/cancelled) status. This is what the
     * rider app should poll every few seconds while online to discover new assignments.
     */
    public java.util.Optional<DeliveryRequest> findActiveTripForRider(Long riderId) {
        return deliveryRequestRepository.findByRiderIdOrderByCreatedAtDesc(riderId).stream()
                .filter(t -> ACTIVE_RIDER_STATUSES.contains(t.getStatus()))
                .findFirst();
    }

    public java.util.List<DeliveryRequest> findAllTripsForRider(Long riderId) {
        return deliveryRequestRepository.findByRiderIdOrderByCreatedAtDesc(riderId);
    }

    // ---- internals ----

    private void transition(DeliveryRequest trip, TripStatus to) {
        transition(trip, to, null, null);
    }

    private void transition(DeliveryRequest trip, TripStatus to, BigDecimal lat, BigDecimal lng) {
        TripStatus from = trip.getStatus();
        Set<TripStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException("Delivery trip", from.name(), to.name());
        }
        trip.setStatus(to);
        deliveryRequestRepository.save(trip);
        recordHistory(trip.getId(), to, lat, lng);
    }

    private void recordHistory(Long tripId, TripStatus status, BigDecimal lat, BigDecimal lng) {
        TripStatusHistory history = new TripStatusHistory();
        history.setTripId(tripId);
        history.setStatus(status);
        history.setLat(lat);
        history.setLng(lng);
        historyRepository.save(history);
    }

    private String generateOtp() {
        return String.format("%04d", random.nextInt(10000));
    }

    private DeliveryRequest getTrip(Long tripId) {
        return deliveryRequestRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("TRIP_NOT_FOUND", "Delivery request not found: " + tripId));
    }

    private DeliveryRequest getTripOwnedByRider(Long tripId, Long riderId) {
        DeliveryRequest trip = getTrip(tripId);
        if (!riderId.equals(trip.getRiderId())) {
            throw ApiException.forbidden("NOT_YOUR_TRIP", "This trip is not assigned to you");
        }
        return trip;
    }

    private VehicleType getVehicleType(Long vehicleTypeId) {
        return vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Invalid vehicle type"));
    }
}