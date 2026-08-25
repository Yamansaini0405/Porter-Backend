package com.porterclone.delivery.dto;

import java.math.BigDecimal;

public record VehicleFareOption(
        Long vehicleTypeId,
        String vehicleTypeName,
        String imageUrl,
        BigDecimal capacityKg,
        BigDecimal estimatedDistanceKm,
        BigDecimal estimatedDurationMin,
        BigDecimal estimatedFare
) {}