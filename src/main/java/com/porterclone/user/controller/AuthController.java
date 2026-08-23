package com.porterclone.user.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.security.UserPrincipal;
import com.porterclone.user.dto.AuthResponse;
import com.porterclone.user.dto.LoginRequest;
import com.porterclone.user.dto.OtpRequest;
import com.porterclone.user.dto.PasswordLoginRequest;
import com.porterclone.user.dto.RegisterRequest;
import com.porterclone.user.dto.SetPasswordRequest;
import com.porterclone.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /** Login via OTP (phone + otp). */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.verifyOtpAndLogin(request.phone(), request.otp()));
    }

    /** Login via password (phone + password) — alternative to OTP login. */
    @PostMapping("/login/password")
    public ApiResponse<AuthResponse> loginWithPassword(@Valid @RequestBody PasswordLoginRequest request) {
        return ApiResponse.ok(authService.loginWithPassword(request.phone(), request.password()));
    }

    /**
     * Set or change the password for the currently authenticated user.
     * Works both for adding a password to an OTP-only account and for changing an existing one
     * (in which case currentPassword must be supplied and correct).
     */
    @PostMapping("/password/set")
    public ApiResponse<Void> setPassword(@Valid @RequestBody SetPasswordRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        authService.setPassword(principal.userId(), request);
        return ApiResponse.ok(null, "Password updated successfully");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refreshAccessToken(refreshToken));
    }
}
