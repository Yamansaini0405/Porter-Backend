package com.porterclone.delivery.dto;

import java.math.BigDecimal;

/** One row in the vehicle-selection screen: what it looks like + what it'll cost. */
public record VehicleFareOption(
        Long vehicleTypeId,
        String vehicleTypeName,
        BigDecimal capacityKg,
        BigDecimal estimatedDistanceKm,
        BigDecimal estimatedDurationMin,
        BigDecimal estimatedFare
) {}
