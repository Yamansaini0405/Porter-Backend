package com.porterclone.user.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.user.dto.AuthResponse;
import com.porterclone.user.dto.LoginRequest;
import com.porterclone.user.dto.OtpRequest;
import com.porterclone.user.dto.RegisterRequest;
import com.porterclone.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(null, "Registered successfully. OTP sent for verification.");
    }

    @PostMapping("/otp/request")
    public ApiResponse<Void> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestLoginOtp(request.phone());
        return ApiResponse.ok(null, "OTP sent");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.verifyOtpAndLogin(request.phone(), request.otp()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refreshAccessToken(refreshToken));
    }
}
