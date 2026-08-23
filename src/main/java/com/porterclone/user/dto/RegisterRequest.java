package com.porterclone.user.dto;

import com.porterclone.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number") String phone,
        @NotBlank String name,
        @NotNull Role role,           // CUSTOMER or RIDER only — ADMIN created separately
        String email,

        // Optional: if provided, the user can log in later with either password or OTP.
        // If omitted, the account is OTP-only until the user sets a password via /auth/password/set.
        @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters") String password
) {}
