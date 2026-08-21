package com.porterclone.admin.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.delivery.entity.TripStatus;
import com.porterclone.delivery.repository.DeliveryRequestRepository;
import com.porterclone.rider.entity.OnboardingStatus;
import com.porterclone.rider.repository.RiderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/** Admin panel: high-level operational snapshot. Extend with date-range filters/exports as needed. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final RiderRepository riderRepository;

    public AdminDashboardController(DeliveryRequestRepository deliveryRequestRepository,
                                     RiderRepository riderRepository) {
        this.deliveryRequestRepository = deliveryRequestRepository;
        this.riderRepository = riderRepository;
    }

    public record DashboardSummary(
            long totalTrips,
            Map<TripStatus, Long> tripsByStatus,
            long totalRiders,
            Map<OnboardingStatus, Long> ridersByOnboardingStatus
    ) {}

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        var trips = deliveryRequestRepository.findAll();
        var riders = riderRepository.findAll();

        Map<TripStatus, Long> tripsByStatus = trips.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus(), Collectors.counting()));

        Map<OnboardingStatus, Long> ridersByStatus = riders.stream()
                .collect(Collectors.groupingBy(r -> r.getOnboardingStatus(), Collectors.counting()));

        return ApiResponse.ok(new DashboardSummary(trips.size(), tripsByStatus, riders.size(), ridersByStatus));
    }
}
