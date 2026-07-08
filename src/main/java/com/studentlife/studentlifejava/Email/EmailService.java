package com.studentlife.studentlifejava.Email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender  mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendOtpEmail(String to, String otp, long validityMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Your StudentLife verification code");
        message.setText(
                "Your verification code is: " + otp + "\n\n"
                        + "It expires in " + validityMinutes + " minutes.\n"
                        + "If you didn't request this, you can ignore this email."
        );

        mailSender.send(message);
    }
}
