package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.Email.EmailService;
import com.studentlife.studentlifejava.exception.ApiException;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.service.VerificationService;
import com.studentlife.studentlifejava.utils.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final String REDIS_PREFIX = "OTP:";
    private static final String PASSWORD_RESET_PREFIX = "PASSWORD_RESET:";

    @Value("${app.security.otp.validity-minutes}")
    private long otpValidityMinutes;

    @Value("${app.security.reset-token.validity-minutes}")
    private long resetTokenValidityMinutes;

    @Override
    public void generateAndSaveOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new ApiException(404, "No account found for this email");
        }

        String key = REDIS_PREFIX + email;
        String otp = OtpGenerator.generateOtp();

        stringRedisTemplate.opsForValue().set(key, otp, otpValidityMinutes, TimeUnit.MINUTES);

        emailService.sendOtpEmail(email, otp, otpValidityMinutes);
    }

    @Override
    public boolean validateAndDestroyOtp(String email, String clientOtp) {
        String key = REDIS_PREFIX + email;

        // Only fetch here - deleting on every attempt would let a single mistyped
        // digit burn the OTP and force the user to request a brand new one.
        String serverOtp = stringRedisTemplate.opsForValue().get(key);
        if (serverOtp == null) {
            return false;
        }

        // Constant-time compare: a 6-digit OTP has few enough possibilities that
        // String.equals' early-exit-on-mismatch could leak timing information.
        boolean matches = MessageDigest.isEqual(
                serverOtp.getBytes(StandardCharsets.UTF_8),
                clientOtp.getBytes(StandardCharsets.UTF_8)
        );

        // Only consume the OTP once it's actually been used successfully.
        if (matches) {
            stringRedisTemplate.delete(key);
        }
        return matches;
    }

    @Override
    public String generateResetToken(String email) {

        String token = UUID.randomUUID().toString();

        stringRedisTemplate.opsForValue().set(
                PASSWORD_RESET_PREFIX + token,
                email,
                resetTokenValidityMinutes,
                TimeUnit.MINUTES
        );

        return token;
    }

    @Override
    public String consumeResetToken(String token) {

        return stringRedisTemplate.opsForValue()
                .getAndDelete(PASSWORD_RESET_PREFIX + token);
    }
}
