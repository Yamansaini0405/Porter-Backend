package com.porterclone.delivery.dto;

import java.math.BigDecimal;

public record FareEstimateResponse(
        BigDecimal estimatedDistanceKm,
        BigDecimal estimatedDurationMin,
        BigDecimal estimatedFare
) {}
