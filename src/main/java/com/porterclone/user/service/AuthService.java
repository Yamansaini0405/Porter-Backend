package com.porterclone.user.service;

import com.porterclone.common.exception.ApiException;
import com.porterclone.customer.entity.Customer;
import com.porterclone.customer.repository.CustomerRepository;
import com.porterclone.rider.entity.OnboardingStatus;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import com.porterclone.security.JwtService;
import com.porterclone.user.dto.AuthResponse;
import com.porterclone.user.dto.CustomerSummaryResponse;
import com.porterclone.user.dto.RegisterRequest;
import com.porterclone.user.dto.RiderSummaryResponse;
import com.porterclone.user.dto.SetPasswordRequest;
import com.porterclone.user.dto.UserSummaryResponse;
import com.porterclone.user.entity.AccountStatus;
import com.porterclone.user.entity.RefreshToken;
import com.porterclone.user.entity.Role;
import com.porterclone.user.entity.User;
import com.porterclone.user.repository.RefreshTokenRepository;
import com.porterclone.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RiderRepository riderRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       RiderRepository riderRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       OtpService otpService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.riderRepository = riderRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (request.role() == Role.ADMIN || request.role() == Role.SUPPORT) {
            throw ApiException.forbidden("SELF_REGISTRATION_NOT_ALLOWED", "This role cannot self-register");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw ApiException.conflict("PHONE_ALREADY_REGISTERED", "An account with this phone number already exists");
        }

        User user = new User();
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setAccountStatus(AccountStatus.ACTIVE);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user = userRepository.save(user);

        if (request.role() == Role.CUSTOMER) {
            Customer customer = new Customer();
            customer.setUserId(user.getId());
            customer.setName(request.name());
            customerRepository.save(customer);
        } else { // RIDER
            Rider rider = new Rider();
            rider.setUserId(user.getId());
            rider.setName(request.name());
            rider.setOnboardingStatus(OnboardingStatus.REGISTERED); // rider is NOT active/available yet
            riderRepository.save(rider);
        }

        otpService.sendOtp(request.phone());
    }

    public void requestLoginOtp(String phone) {
        if (!userRepository.existsByPhone(phone)) {
            throw ApiException.notFound("USER_NOT_FOUND", "No account found with this phone number");
        }
        otpService.sendOtp(phone);
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(String phone, String otp) {
        if (!otpService.verifyOtp(phone, otp)) {
            throw ApiException.badRequest("INVALID_OTP", "The OTP you entered is incorrect");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "No account found with this phone number"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("ACCOUNT_NOT_ACTIVE", "Your account is " + user.getAccountStatus());
        }

        user.setPhoneVerified(true);
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse loginWithPassword(String phone, String password) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "No account found with this phone number"));

        if (user.getPasswordHash() == null) {
            throw ApiException.badRequest("PASSWORD_NOT_SET", "This account has no password set. Please log in with OTP instead.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_CREDENTIALS", "Incorrect phone number or password");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("ACCOUNT_NOT_ACTIVE", "Your account is " + user.getAccountStatus());
        }

        return issueTokens(user);
    }

    /**
     * Sets or changes the password for the authenticated user.
     * If the user has no password yet (OTP-only account), currentPassword is not required.
     * If the user already has a password, currentPassword must match before it can be changed.
     */
    @Transactional
    public void setPassword(Long userId, SetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User no longer exists"));

        if (user.getPasswordHash() != null) {
            if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenPlain) {
        String hash = sha256(refreshTokenPlain);

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is invalid or has been revoked"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED", "Refresh token has expired. Please log in again");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User no longer exists"));

        // Rotate: revoke the used refresh token and issue a fresh pair (prevents replay).
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshTokenPlain = generateSecureRandomToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(sha256(refreshTokenPlain));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS));
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user, accessToken, refreshTokenPlain);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshTokenPlain) {
        UserSummaryResponse userSummary = new UserSummaryResponse(
                user.getId(),
                user.getPhone(),
                user.getEmail(),
                user.getRole().name(),
                user.getAccountStatus(),
                user.isPhoneVerified(),
                user.getPasswordHash() != null
        );

        CustomerSummaryResponse customerSummary = null;
        RiderSummaryResponse riderSummary = null;
        Long customerId = null;
        Long riderId = null;

        if (user.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
            if (customer != null) {
                customerId = customer.getId();
                customerSummary = new CustomerSummaryResponse(
                        customer.getId(),
                        customer.getUserId(),
                        customer.getName(),
                        customer.getRatingAvg(),
                        customer.getTotalTrips()
                );
            }
        } else if (user.getRole() == Role.RIDER) {
            Rider rider = riderRepository.findByUserId(user.getId()).orElse(null);
            if (rider != null) {
                riderId = rider.getId();
                riderSummary = new RiderSummaryResponse(
                        rider.getId(),
                        rider.getUserId(),
                        rider.getName(),
                        rider.getOnboardingStatus(),
                        rider.getOnlineStatus(),
                        rider.getAvailabilityStatus(),
                        rider.getCurrentVehicleId(),
                        rider.getRatingAvg(),
                        rider.getTotalTrips(),
                        rider.getWalletBalance()
                );
            }
        }

        return new AuthResponse(
                user.getId(),
                customerId,
                riderId,
                user.getRole().name(),
                accessToken,
                refreshTokenPlain,
                userSummary,
                customerSummary,
                riderSummary
        );
    }

    private String generateSecureRandomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
