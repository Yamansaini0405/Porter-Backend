package com.porterclone.admin.dto;

import com.porterclone.commission.entity.CommissionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CommissionConfigRequest(
        Long vehicleTypeId,          // null = global default
        @NotNull CommissionType commissionType,
        @NotNull BigDecimal value
) {}
