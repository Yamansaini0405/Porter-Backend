package com.porterclone.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        Long userId,
        Long customerId,
        Long riderId,
        String role,
        String accessToken,
        String refreshToken,
        UserSummaryResponse user,
        CustomerSummaryResponse customer,
        RiderSummaryResponse rider
) {}
