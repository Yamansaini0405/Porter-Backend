package com.porterclone.user.service;

import com.porterclone.common.exception.ApiException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * OTP generation/verification backed by Redis (TTL handles expiry automatically).
 * In production, sendOtp() would call an SMS gateway (Twilio/MSG91) — currently logs
 * to console for local development so you can test the flow without a paid SMS provider.
 */
@Service
public class OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void sendOtp(String phone) {
        String otp = String.format("%04d", random.nextInt(10000));
        String hashKey = "otp:hash:" + phone;
        String attemptsKey = "otp:attempts:" + phone;

        redisTemplate.opsForValue().set(hashKey, encoder.encode(otp), OTP_TTL);
        redisTemplate.delete(attemptsKey);

        // TODO: integrate real SMS gateway (Twilio / MSG91 / SNS) here.
        System.out.printf("[DEV-ONLY] OTP for %s is %s (expires in 5 min)%n", phone, otp);
    }

    public boolean verifyOtp(String phone, String otp) {
        String hashKey = "otp:hash:" + phone;
        String attemptsKey = "otp:attempts:" + phone;

        String storedHash = redisTemplate.opsForValue().get(hashKey);
        if (storedHash == null) {
            throw ApiException.badRequest("OTP_EXPIRED", "OTP has expired or was not requested. Please request a new one.");
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, OTP_TTL);
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(hashKey);
            throw ApiException.badRequest("OTP_LOCKED", "Too many incorrect attempts. Please request a new OTP.");
        }

        boolean valid = encoder.matches(otp, storedHash);
        if (valid) {
            redisTemplate.delete(hashKey);
            redisTemplate.delete(attemptsKey);
        }
        return valid;
    }
}
