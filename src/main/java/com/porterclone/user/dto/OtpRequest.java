package com.porterclone.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequest(
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number") String phone
) {}
