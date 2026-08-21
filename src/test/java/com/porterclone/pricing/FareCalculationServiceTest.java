package com.porterclone.pricing;

import com.porterclone.pricing.dto.FareBreakdown;
import com.porterclone.pricing.dto.FareCalculationRequest;
import com.porterclone.pricing.service.FareCalculationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FareCalculationService is a pure function (no Spring context needed), so this
 * test runs in milliseconds with plain JUnit — no @SpringBootTest required.
 */
class FareCalculationServiceTest {

    private final FareCalculationService service = new FareCalculationService();

    @Test
    void noWaitingTime_withinFreeWindow_addsNoWaitingCharge() {
        LocalDateTime arrived = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime started = arrived.plusMinutes(4); // under 5-min free window

        FareBreakdown result = service.calculate(new FareCalculationRequest(
                new BigDecimal("25.00"), new BigDecimal("8.00"), new BigDecimal("1.00"),
                5, new BigDecimal("1.50"),
                new BigDecimal("10.00"), new BigDecimal("24.00"),
                arrived, started,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));

        assertEquals(0, result.chargeableWaitMinutes());
        assertEquals(new BigDecimal("0.00"), result.waitingCharge());
    }

    @Test
    void customerHoldsVehicleBeyondFreeWindow_chargesForExtraMinutesOnly() {
        LocalDateTime arrived = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime started = arrived.plusMinutes(20); // 15 min free -> 5 chargeable (bike-style config below uses 5 free)

        FareBreakdown result = service.calculate(new FareCalculationRequest(
                new BigDecimal("25.00"), new BigDecimal("8.00"), new BigDecimal("1.00"),
                5, new BigDecimal("1.50"),           // free_wait=5min, ₹1.50/min after that
                new BigDecimal("10.00"), new BigDecimal("24.00"),
                arrived, started,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));

        // 20 actual minutes - 5 free = 15 chargeable minutes
        assertEquals(15, result.chargeableWaitMinutes());
        assertEquals(new BigDecimal("22.50"), result.waitingCharge()); // 15 * 1.50
    }

    @Test
    void grossFare_includesAllComponents() {
        FareBreakdown result = service.calculate(new FareCalculationRequest(
                new BigDecimal("80.00"),   // base
                new BigDecimal("18.00"),   // per km
                new BigDecimal("2.00"),    // per min
                15, new BigDecimal("3.00"),
                new BigDecimal("5.00"),    // 5 km
                new BigDecimal("12.00"),   // 12 min
                null, null,                // no waiting
                BigDecimal.ONE, new BigDecimal("10.00"), new BigDecimal("5.00"), new BigDecimal("5.00")
        ));

        // base 80 + distance(5*18=90) + time(12*2=24) = 194, + toll 10 - discount 5 = 199
        // tax 5% of 199 = 9.95 -> gross = 208.95
        assertEquals(new BigDecimal("208.95"), result.grossFare());
    }
}
