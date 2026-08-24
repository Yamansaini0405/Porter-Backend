package com.porterclone.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

/**
 * Pickup/drop only — no vehicle type. Used for the "show me fares for every
 * vehicle option" screen that customers see BEFORE picking a vehicle.
 */
public record LocationOnlyRequest(
        @NotBlank String pickupAddress,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal pickupLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal pickupLng,
        @NotBlank String dropAddress,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal dropLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal dropLng
) {}
