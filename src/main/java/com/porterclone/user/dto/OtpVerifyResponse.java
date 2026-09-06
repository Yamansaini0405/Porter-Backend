package com.porterclone.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result of POST /api/v1/auth/otp/verify.
 *
 * newUser = false -> phone belongs to an existing account: `auth` is populated (full JWT pair),
 *                     client goes straight Home.
 * newUser = true  -> phone has never registered before: `registrationToken` is populated instead.
 *                     Client shows "Enter Name" screen, then calls POST /api/v1/auth/profile with
 *                     that token in the Authorization header to finish creating the account.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpVerifyResponse(
        boolean newUser,
        String registrationToken,
        AuthResponse auth
) {
    public static OtpVerifyResponse existingUser(AuthResponse auth) {
        return new OtpVerifyResponse(false, null, auth);
    }

    public static OtpVerifyResponse newUser(String registrationToken) {
        return new OtpVerifyResponse(true, registrationToken, null);
    }
}