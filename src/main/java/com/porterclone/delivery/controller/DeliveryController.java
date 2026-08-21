package com.porterclone.delivery.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.delivery.dto.CreateDeliveryRequest;
import com.porterclone.delivery.dto.FareEstimateResponse;
import com.porterclone.delivery.dto.LocationOnlyRequest;
import com.porterclone.delivery.dto.VehicleFareOption;
import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // ---- Customer flow ----

    /**
     * Step 1: customer has only entered pickup + drop so far. Returns a fare estimate
     * for EVERY active vehicle type so the app can render the "choose your vehicle" screen.
     */
    @PostMapping("/estimate-all")
    public ApiResponse<List<VehicleFareOption>> estimateAll(@Valid @RequestBody LocationOnlyRequest request) {
        return ApiResponse.ok(deliveryService.estimateFareForAllVehicleTypes(request));
    }

    /** Re-quotes a single vehicle type — useful if the customer changes their mind on the same screen. */
    @PostMapping("/estimate")
    public ApiResponse<FareEstimateResponse> estimate(@Valid @RequestBody CreateDeliveryRequest request) {
        return ApiResponse.ok(deliveryService.estimateFare(request));
    }

    @PostMapping("/customers/{customerId}")
    public ApiResponse<DeliveryRequest> create(@PathVariable Long customerId,
                                                @Valid @RequestBody CreateDeliveryRequest request) {
        DeliveryRequest trip = deliveryService.createRequest(customerId, request);
        // Broadcasts to every eligible nearby rider and returns immediately — the trip
        // comes back as SEARCHING_RIDER with riderId still null. The customer app should
        // watch /topic/trip/{tripId}/status (or poll GET /{tripId}) for RIDER_ASSIGNED.
        return ApiResponse.ok(deliveryService.dispatch(trip.getId()));
    }

    @PostMapping("/{tripId}/cancel")
    public ApiResponse<DeliveryRequest> cancel(@PathVariable Long tripId,
                                                @RequestParam String cancelledBy,
                                                @RequestParam(required = false) String reason) {
        return ApiResponse.ok(deliveryService.cancel(tripId, cancelledBy, reason));
    }

    // ---- Rider flow ----

    /**
     * What the rider app polls to find out "do I have a request?". Returns the rider's
     * current active trip (assigned/arrived/in-transit) if one exists, or null if not —
     * this is the piece that was completely missing: a rider had no way to discover a
     * trip had been assigned to them.
     */
    @GetMapping("/riders/{riderId}/current")
    public ApiResponse<DeliveryRequest> currentTripForRider(@PathVariable Long riderId) {
        return ApiResponse.ok(deliveryService.findActiveTripForRider(riderId).orElse(null));
    }

    /** Full trip history for a rider — completed, cancelled, everything. */
    @GetMapping("/riders/{riderId}")
    public ApiResponse<List<DeliveryRequest>> tripsForRider(@PathVariable Long riderId) {
        return ApiResponse.ok(deliveryService.findAllTripsForRider(riderId));
    }

    /**
     * Polling fallback for the broadcast — returns every SEARCHING_RIDER request within
     * this rider's radius and matching vehicle type. Use the WebSocket push
     * (/topic/rider/{riderId}/requests) as the primary path; poll this if the socket
     * connection was dropped or the app was backgrounded.
     */
    @GetMapping("/riders/{riderId}/pending")
    public ApiResponse<List<DeliveryRequest>> pendingRequestsForRider(@PathVariable Long riderId) {
        return ApiResponse.ok(deliveryService.findPendingTripsForRider(riderId));
    }

    /**
     * First rider to call this on a given trip wins it — guarded by a distributed lock
     * (see RiderMatchingService.tryAssignRider). Everyone else gets TRIP_ALREADY_ASSIGNED.
     */
    @PostMapping("/{tripId}/riders/{riderId}/accept")
    public ApiResponse<DeliveryRequest> acceptTrip(@PathVariable Long tripId, @PathVariable Long riderId) {
        return ApiResponse.ok(deliveryService.acceptTrip(tripId, riderId));
    }

    @PostMapping("/{tripId}/riders/{riderId}/arrived")
    public ApiResponse<DeliveryRequest> riderArrived(@PathVariable Long tripId, @PathVariable Long riderId,
                                                       @RequestParam BigDecimal lat, @RequestParam BigDecimal lng) {
        return ApiResponse.ok(deliveryService.markRiderArrived(tripId, riderId, lat, lng));
    }

    @PostMapping("/{tripId}/riders/{riderId}/start")
    public ApiResponse<DeliveryRequest> startTrip(@PathVariable Long tripId, @PathVariable Long riderId,
                                                    @RequestParam String otp) {
        return ApiResponse.ok(deliveryService.startTrip(tripId, riderId, otp));
    }

    @PostMapping("/{tripId}/riders/{riderId}/arrived-drop")
    public ApiResponse<DeliveryRequest> arrivedAtDrop(@PathVariable Long tripId, @PathVariable Long riderId,
                                                        @RequestParam BigDecimal lat, @RequestParam BigDecimal lng) {
        return ApiResponse.ok(deliveryService.markArrivedAtDrop(tripId, riderId, lat, lng));
    }

    @PostMapping("/{tripId}/riders/{riderId}/complete")
    public ApiResponse<DeliveryRequest> complete(@PathVariable Long tripId, @PathVariable Long riderId) {
        return ApiResponse.ok(deliveryService.completeTrip(tripId, riderId));
    }
}
