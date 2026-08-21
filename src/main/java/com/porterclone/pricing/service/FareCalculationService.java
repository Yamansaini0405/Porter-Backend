package com.porterclone.pricing.service;

import com.porterclone.pricing.dto.FareBreakdown;
import com.porterclone.pricing.dto.FareCalculationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Stateless fare engine. No DB calls here — keeps it trivially unit-testable
 * and reusable both for the customer's up-front ESTIMATE and the FINAL fare at trip completion.
 *
 * gross_fare = base_fare + distance_fare + time_fare + waiting_charge + surge_amount
 *              + toll_charge - discount_amount + tax_amount
 *
 * Waiting-charge rule (the "holds vehicle too long" requirement):
 *   wait clock starts at RIDER_ARRIVED, stops at trip start (IN_TRANSIT).
 *   chargeable_wait_min = max(0, actual_wait_min - free_wait_minutes)
 *   waiting_charge = chargeable_wait_min * wait_charge_per_min
 */
@Service
public class FareCalculationService {

    private static final int SCALE = 2;

    public FareBreakdown calculate(FareCalculationRequest req) {
        BigDecimal baseFare = nz(req.baseFare());
        BigDecimal distanceFare = nz(req.perKmRate()).multiply(nz(req.distanceKm()));
        BigDecimal timeFare = nz(req.perMinRate()).multiply(nz(req.durationMin()));

        long chargeableWaitMinutes = computeChargeableWaitMinutes(
                req.riderArrivedAt(), req.tripStartedAt(), req.freeWaitMinutes());
        BigDecimal waitingCharge = nz(req.waitChargePerMin())
                .multiply(BigDecimal.valueOf(chargeableWaitMinutes));

        BigDecimal preSurgeSubtotal = baseFare.add(distanceFare).add(timeFare).add(waitingCharge);

        BigDecimal surgeMultiplier = req.surgeMultiplier() == null ? BigDecimal.ONE : req.surgeMultiplier();
        BigDecimal surgeAmount = preSurgeSubtotal.multiply(surgeMultiplier.subtract(BigDecimal.ONE))
                .max(BigDecimal.ZERO);

        BigDecimal tollCharge = nz(req.tollCharge());
        BigDecimal discountAmount = nz(req.discountAmount());

        BigDecimal taxableAmount = preSurgeSubtotal.add(surgeAmount).add(tollCharge).subtract(discountAmount)
                .max(BigDecimal.ZERO);
        BigDecimal taxPct = nz(req.taxPercentage());
        BigDecimal taxAmount = taxableAmount.multiply(taxPct).divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);

        BigDecimal grossFare = taxableAmount.add(taxAmount).setScale(SCALE, RoundingMode.HALF_UP);

        return new FareBreakdown(
                round(baseFare), round(distanceFare), round(timeFare), round(waitingCharge),
                chargeableWaitMinutes, round(surgeAmount), round(tollCharge), round(discountAmount),
                round(taxAmount), grossFare
        );
    }

    private long computeChargeableWaitMinutes(LocalDateTime arrivedAt, LocalDateTime startedAt, int freeMinutes) {
        if (arrivedAt == null || startedAt == null || !startedAt.isAfter(arrivedAt)) {
            return 0;
        }
        long actualWaitMinutes = Duration.between(arrivedAt, startedAt).toMinutes();
        return Math.max(0, actualWaitMinutes - freeMinutes);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
