package com.porterclone.rider.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.matching.service.RiderLocationService;
import com.porterclone.rider.dto.DocumentSubmitRequest;
import com.porterclone.rider.dto.LocationPingRequest;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.entity.RiderDocument;
import com.porterclone.rider.service.RiderAvailabilityService;
import com.porterclone.rider.service.RiderOnboardingService;
import com.porterclone.security.UserPrincipal;
import com.porterclone.vehicle.service.VehicleService;
import com.porterclone.websocket.LocationBroadcastService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Rider-app-facing endpoints. For self lookup we resolve rider profile from authenticated userId. */
@RestController
@RequestMapping("/api/v1/riders")
public class RiderController {

    private final RiderOnboardingService onboardingService;
    private final RiderAvailabilityService availabilityService;
    private final RiderLocationService locationService;
    private final LocationBroadcastService broadcastService;
    private final VehicleService vehicleService;

    public RiderController(RiderOnboardingService onboardingService,
                            RiderAvailabilityService availabilityService,
                            RiderLocationService locationService,
                            LocationBroadcastService broadcastService,
                            VehicleService vehicleService) {
        this.onboardingService = onboardingService;
        this.availabilityService = availabilityService;
        this.locationService = locationService;
        this.broadcastService = broadcastService;
        this.vehicleService = vehicleService;
    }

    @PostMapping("/{riderId}/documents")
    public ApiResponse<RiderDocument> submitDocument(@PathVariable Long riderId,
                                                       @Valid @RequestBody DocumentSubmitRequest request) {
        return ApiResponse.ok(onboardingService.submitDocument(riderId, request.docType(), request.fileUrl()));
    }

    @PostMapping("/{riderId}/submit-for-review")
    public ApiResponse<Rider> submitForReview(@PathVariable Long riderId) {
        return ApiResponse.ok(onboardingService.submitForReview(riderId));
    }

    @PostMapping("/{riderId}/activate")
    public ApiResponse<Rider> activate(@PathVariable Long riderId) {
        return ApiResponse.ok(onboardingService.activate(riderId));
    }

    /** Rider picks which of their VERIFIED vehicles they're driving this shift. Required before /online. */
    @PostMapping("/{riderId}/vehicles/{vehicleId}/select-active")
    public ApiResponse<Void> selectActiveVehicle(@PathVariable Long riderId, @PathVariable Long vehicleId) {
        vehicleService.selectActiveVehicle(riderId, vehicleId);
        return ApiResponse.ok(null, "Active vehicle set");
    }

    @PostMapping("/{riderId}/online")
    public ApiResponse<Rider> goOnline(@PathVariable Long riderId) {
        return ApiResponse.ok(availabilityService.goOnline(riderId));
    }

    @PostMapping("/{riderId}/offline")
    public ApiResponse<Rider> goOffline(@PathVariable Long riderId) {
        return ApiResponse.ok(availabilityService.goOffline(riderId));
    }

    @PostMapping("/{riderId}/break")
    public ApiResponse<Rider> takeBreak(@PathVariable Long riderId) {
        return ApiResponse.ok(availabilityService.takeBreak(riderId));
    }

    /** High-frequency endpoint (every 4-5s from the rider app). Feeds Redis geo + heartbeat.
     *  Vehicle type is resolved server-side from the rider's selected active vehicle — see LocationPingRequest. */
    @PostMapping("/{riderId}/location")
    public ApiResponse<Void> updateLocation(@PathVariable Long riderId,
                                             @Valid @RequestBody LocationPingRequest request) {
        locationService.updateLocation(riderId, request.lat(), request.lng());

        if (request.activeTripId() != null) {
            broadcastService.broadcastLocation(request.activeTripId(), riderId, request.lat(), request.lng());
        }
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<Rider> me(@AuthenticationPrincipal UserPrincipal principal) {
        Rider rider = onboardingService.getRiderByUserId(principal.userId());
        return ApiResponse.ok(rider);
    }
}
