package com.porterclone.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Pickup/drop only — no vehicle type. Used for the "show me fares for every
 * vehicle option" screen that customers see BEFORE picking a vehicle.
 */
public record LocationOnlyRequest(
        @NotBlank String pickupAddress,
        @NotNull BigDecimal pickupLat,
        @NotNull BigDecimal pickupLng,
        @NotBlank String dropAddress,
        @NotNull BigDecimal dropLat,
        @NotNull BigDecimal dropLng
) {}
