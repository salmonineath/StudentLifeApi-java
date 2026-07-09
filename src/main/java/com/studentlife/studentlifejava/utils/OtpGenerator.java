package com.studentlife.studentlifejava.utils;

import java.security.SecureRandom;
import java.util.stream.Collectors;

public class OtpGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    public static String generateOtp() {
        return secureRandom.ints(OTP_LENGTH, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}
