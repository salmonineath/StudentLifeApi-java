package com.studentlife.studentlifejava.utils;

import java.security.SecureRandom;
import java.util.stream.Collectors;

public class OtpGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    // Generates 6 independent random digits (0-9) and concatenates them, e.g.
    // "042817" - deliberately digit-by-digit rather than
    // String.format("%06d", nextInt(1_000_000)) so leading zeros fall out
    // naturally instead of relying on zero-padding.
    public static String generateOtp() {
        return secureRandom.ints(OTP_LENGTH, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}
