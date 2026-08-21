package com.porterclone.pricing.dto;

import java.math.BigDecimal;

/** Pure calculation result — no DB/side effects. Returned to the caller to persist as needed. */
public record FareBreakdown(
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal waitingCharge,
        long chargeableWaitMinutes,
        BigDecimal surgeAmount,
        BigDecimal tollCharge,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal grossFare
) {}
