package com.porterclone.rider.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.common.service.FileStorageService;
import com.porterclone.matching.service.RiderLocationService;
import com.porterclone.rider.dto.LocationPingRequest;
import com.porterclone.rider.entity.DocumentType;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.entity.RiderDocument;
import com.porterclone.rider.service.RiderAvailabilityService;
import com.porterclone.rider.service.RiderOnboardingService;
import com.porterclone.security.UserPrincipal;
import com.porterclone.vehicle.service.VehicleService;
import com.porterclone.websocket.LocationBroadcastService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Rider-app-facing endpoints. For self lookup we resolve rider profile from authenticated userId. */
@RestController
@RequestMapping("/api/v1/riders")
public class RiderController {

    private final RiderOnboardingService onboardingService;
    private final RiderAvailabilityService availabilityService;
    private final RiderLocationService locationService;
    private final LocationBroadcastService broadcastService;
    private final VehicleService vehicleService;
    private final FileStorageService fileStorageService;

    public RiderController(RiderOnboardingService onboardingService,
                           RiderAvailabilityService availabilityService,
                           RiderLocationService locationService,
                           LocationBroadcastService broadcastService,
                           VehicleService vehicleService,
                           FileStorageService fileStorageService) {
        this.onboardingService = onboardingService;
        this.availabilityService = availabilityService;
        this.locationService = locationService;
        this.broadcastService = broadcastService;
        this.vehicleService = vehicleService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Rider uploads a KYC document (license, RC, Aadhar, PAN, insurance, photo) as a file.
     *
     * Content-Type: multipart/form-data
     * Form fields: docType (text), file (binary)
     */
    @PostMapping(value = "/{riderId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RiderDocument> submitDocument(@PathVariable Long riderId,
                                                     @RequestParam("docType") DocumentType docType,
                                                     @RequestPart("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeRiderDocument(riderId, file);
        return ApiResponse.ok(onboardingService.submitDocument(riderId, docType, fileUrl));
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