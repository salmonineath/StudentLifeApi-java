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

        String serverOtp = stringRedisTemplate.opsForValue().getAndDelete(key);
        return serverOtp != null && serverOtp.equals(clientOtp);
    }

    @Override
    public String generateResetToken(String email) {

        String token = UUID.randomUUID().toString();

        stringRedisTemplate.opsForValue().set(
                PASSWORD_RESET_PREFIX + token,
                email,
                10,
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
