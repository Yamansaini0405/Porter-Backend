package com.porterclone.zone.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LocationRequest(
        @NotNull @DecimalMin(value="-90.0") @DecimalMax(value="90.0")
        BigDecimal latitude,
        @NotNull @DecimalMin(value="-180.0") @DecimalMax(value="180.0")
        BigDecimal longitude,
        String address, String city, String state, String pincode, String country
) {}
