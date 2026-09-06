package com.porterclone.user.dto;

import com.porterclone.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/auth/profile — called with the short-lived registration token
 * (from OtpVerifyResponse.registrationToken) to finish creating a brand-new account.
 * The phone number itself is NOT taken from the request body — it comes from the verified
 * registration token, so it can't be spoofed.
 */
public record CompleteProfileRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull Role role,           // CUSTOMER or RIDER only — ADMIN/SUPPORT created separately
        String email
) {}