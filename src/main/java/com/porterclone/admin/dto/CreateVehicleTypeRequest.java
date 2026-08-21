package com.porterclone.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateVehicleTypeRequest(
        @NotBlank String name,
        BigDecimal capacityKg,
        @NotNull BigDecimal baseFare,
        @NotNull BigDecimal perKmRate,
        @NotNull BigDecimal perMinRate,
        int freeWaitMinutes,
        @NotNull BigDecimal waitChargePerMin
) {}
