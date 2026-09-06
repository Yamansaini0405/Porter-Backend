package com.porterclone.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number") String phone,
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "OTP must be 4 digits") String otp
) {}