package com.porterclone.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordLoginRequest(
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number") String phone,
        @NotBlank String password
) {}
