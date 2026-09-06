package com.porterclone.security;

/**
 * Lightweight authenticated context extracted from the JWT — attached to SecurityContext.
 *
 * For a normal ACCESS token: userId is set, role is the real role (CUSTOMER/RIDER/ADMIN/SUPPORT),
 * phone is null.
 * For a REGISTRATION token (brand-new phone, OTP verified, account not created yet): userId is
 * null, role is "PENDING", phone is the verified phone number — used by POST /api/v1/auth/profile
 * to know which phone to create the account for.
 */
public record UserPrincipal(Long userId, String role, String phone) {
}