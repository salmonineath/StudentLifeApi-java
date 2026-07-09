package com.studentlife.studentlifejava.service;

public interface VerificationService {

    void generateAndSaveOtp(String  email);

    boolean validateAndDestroyOtp(String email, String otp);

    String generateResetToken(String email);

    String consumeResetToken(String token);
}
