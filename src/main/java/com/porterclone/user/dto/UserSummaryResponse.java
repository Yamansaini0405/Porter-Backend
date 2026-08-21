package com.porterclone.user.dto;

import com.porterclone.user.entity.AccountStatus;

public record UserSummaryResponse(
        Long userId,
        String phone,
        String email,
        String role,
        AccountStatus accountStatus,
        boolean phoneVerified
) {}

