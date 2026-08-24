package com.porterclone.zone.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.zone.dto.*;
import com.porterclone.zone.service.ZoneService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/serviceability")
public class ZoneController {
    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) { this.zoneService = zoneService; }

    @PostMapping("/check")
    public ApiResponse<ServiceabilityResponse> check(@Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok(zoneService.resolveServiceableZone(request.latitude(), request.longitude())
                .map(z -> new ServiceabilityResponse(true, z.getId(), z.getCode(), z.getName(),
                        "Service is available in this area"))
                .orElseGet(() -> new ServiceabilityResponse(false, null, null, null,
                        "Service is not available in this area")));
    }

    @PostMapping("/check-pickup-drop")
    public ApiResponse<PickupDropServiceabilityResponse> checkPickupDrop(
            @Valid @RequestBody PickupDropServiceabilityRequest request) {
        var pickup = zoneService.resolveServiceableZone(request.pickup().latitude(), request.pickup().longitude());
        if (pickup.isEmpty()) {
            return ApiResponse.ok(new PickupDropServiceabilityResponse(false, null, null,
                    "PICKUP_LOCATION_NOT_SERVICEABLE"));
        }

        var drop = zoneService.resolveServiceableZone(request.drop().latitude(), request.drop().longitude());
        if (drop.isEmpty()) {
            return ApiResponse.ok(new PickupDropServiceabilityResponse(false,
                    new ZoneSummaryResponse(pickup.get().getId(), pickup.get().getCode(), pickup.get().getName()),
                    null, "DROP_LOCATION_NOT_SERVICEABLE"));
        }

        return ApiResponse.ok(new PickupDropServiceabilityResponse(true,
                new ZoneSummaryResponse(pickup.get().getId(), pickup.get().getCode(), pickup.get().getName()),
                new ZoneSummaryResponse(drop.get().getId(), drop.get().getCode(), drop.get().getName()), null));
    }
}
