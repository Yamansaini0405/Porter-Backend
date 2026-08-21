package com.porterclone.user.dto;

import com.porterclone.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number") String phone,
        @NotBlank String name,
        @NotNull Role role,           // CUSTOMER or RIDER only — ADMIN created separately
        String email
) {}
