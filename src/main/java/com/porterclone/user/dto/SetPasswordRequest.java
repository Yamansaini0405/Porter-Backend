package com.porterclone.user.dto;

import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        // Required only when the user already has a password set (i.e. changing it).
        // Not required the first time a password is being added to an OTP-only account.
        String currentPassword,

        @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters") String newPassword
) {}
