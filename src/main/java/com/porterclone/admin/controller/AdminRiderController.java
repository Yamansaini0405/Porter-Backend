package com.porterclone.admin.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.rider.dto.RiderDetailsResponse;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.service.RiderOnboardingService;
import com.porterclone.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin panel: rider KYC review queue. Restricted to ROLE_ADMIN via SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin/riders")
public class AdminRiderController {

    private final RiderOnboardingService onboardingService;

    public AdminRiderController(RiderOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/{riderId}/approve")
    public ApiResponse<Rider> approve(@PathVariable Long riderId, @AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(onboardingService.approve(riderId, admin.userId()));
    }

    @PostMapping("/{riderId}/reject")
    public ApiResponse<Rider> reject(@PathVariable Long riderId, @RequestParam String reason,
                                      @AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(onboardingService.reject(riderId, admin.userId(), reason));
    }

    @PostMapping("/{riderId}/suspend")
    public ApiResponse<Rider> suspend(@PathVariable Long riderId, @RequestParam String reason,
                                       @AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(onboardingService.suspend(riderId, admin.userId(), reason));
    }

    @PostMapping("/{riderId}/reinstate")
    public ApiResponse<Rider> reinstate(@PathVariable Long riderId, @AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(onboardingService.reinstate(riderId, admin.userId()));
    }

    @PostMapping("/rider/all")
    public ApiResponse<List<Rider>> getAllRiders(@AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(onboardingService.getAllRiders());
    }

    @GetMapping("/{riderId}")
    public ApiResponse<RiderDetailsResponse> getRiderById(@PathVariable Long riderId,
                                                          @AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(onboardingService.getRiderById(riderId));
    }

}
