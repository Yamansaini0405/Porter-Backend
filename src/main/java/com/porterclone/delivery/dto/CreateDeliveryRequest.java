package com.porterclone.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDeliveryRequest(
        @NotNull Long vehicleTypeId,
        @NotBlank String pickupAddress,
        @NotNull BigDecimal pickupLat,
        @NotNull BigDecimal pickupLng,
        @NotBlank String dropAddress,
        @NotNull BigDecimal dropLat,
        @NotNull BigDecimal dropLng
) {}
