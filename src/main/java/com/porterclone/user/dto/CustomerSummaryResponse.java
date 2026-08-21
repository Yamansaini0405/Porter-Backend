package com.porterclone.user.dto;

import java.math.BigDecimal;

public record CustomerSummaryResponse(
        Long customerId,
        Long userId,
        String name,
        BigDecimal ratingAvg,
        int totalTrips
) {}

