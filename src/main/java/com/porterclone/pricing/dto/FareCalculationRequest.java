package com.porterclone.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Everything the fare engine needs, decoupled from JPA entities so it stays
 * pure/unit-testable. riderArrivedAt/tripStartedAt drive the waiting-charge calc.
 */
public record FareCalculationRequest(
        BigDecimal baseFare,
        BigDecimal perKmRate,
        BigDecimal perMinRate,
        int freeWaitMinutes,
        BigDecimal waitChargePerMin,
        BigDecimal distanceKm,
        BigDecimal durationMin,
        LocalDateTime riderArrivedAt,
        LocalDateTime tripStartedAt,
        BigDecimal surgeMultiplier,     // 1.00 = no surge
        BigDecimal tollCharge,
        BigDecimal discountAmount,
        BigDecimal taxPercentage        // e.g. 5.00 for 5% GST
) {}
