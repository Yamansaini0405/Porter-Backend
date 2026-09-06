package com.porterclone.user.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.common.exception.ApiException;
import com.porterclone.security.JwtService;
import com.porterclone.security.UserPrincipal;
import com.porterclone.user.dto.AuthResponse;
import com.porterclone.user.dto.CompleteProfileRequest;
import com.porterclone.user.dto.OtpRequest;
import com.porterclone.user.dto.OtpVerifyResponse;
import com.porterclone.user.dto.PasswordLoginRequest;
import com.porterclone.user.dto.SetPasswordRequest;
import com.porterclone.user.dto.VerifyOtpRequest;
import com.porterclone.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Uber-style phone-first auth flow:
 *
 *   POST /otp/request  { phone }          -> OTP sent (via Redis), regardless of new/existing user
 *   POST /otp/verify    { phone, otp }    -> existing user: full JWT pair (AuthResponse)
 *                                          -> new user:      short-lived registrationToken
 *   POST /profile  (Bearer <registrationToken>) { name, role, email? }
 *                                          -> creates User + Customer/Rider, returns full JWT pair
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/request")
    public ApiResponse<Void> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request.phone());
        return ApiResponse.ok(null, "OTP sent");
    }

    @PostMapping("/otp/verify")
    public ApiResponse<OtpVerifyResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok(authService.verifyOtp(request.phone(), request.otp()));
    }

    /**
     * Finishes registration for a brand-new phone number. Must be called with the
     * registrationToken returned by /otp/verify in the Authorization header — the phone number is
     * read from that token, never from the request body, so it can't be spoofed.
     */
    @PostMapping("/profile")
    public ApiResponse<AuthResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || !JwtService.ROLE_PENDING.equals(principal.role()) || principal.phone() == null) {
            throw ApiException.unauthorized("INVALID_REGISTRATION_TOKEN",
                    "Registration token is missing, expired, or invalid. Please verify your phone number again.");
        }
        return ApiResponse.ok(authService.completeProfile(principal.phone(), request));
    }

    /** Login via password (phone + password) — alternative to OTP login, for returning users only. */
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