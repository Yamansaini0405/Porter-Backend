package com.porterclone.user.dto;

import com.porterclone.rider.entity.AvailabilityStatus;
import com.porterclone.rider.entity.OnboardingStatus;
import com.porterclone.rider.entity.OnlineStatus;

import java.math.BigDecimal;

public record RiderSummaryResponse(
        Long riderId,
        Long userId,
        String name,
        OnboardingStatus onboardingStatus,
        OnlineStatus onlineStatus,
        AvailabilityStatus availabilityStatus,
        Long currentVehicleId,
        BigDecimal ratingAvg,
        int totalTrips,
        BigDecimal walletBalance
) {}

