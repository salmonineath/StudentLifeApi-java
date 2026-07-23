package com.studentlife.studentlifejava.Email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender  mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendOtpEmail(String to, String otp, long validityMinutes) {
        send(to, "Your StudentLife verification code",
                "Your verification code is: " + otp + "\n\n"
                        + "It expires in " + validityMinutes + " minutes.\n"
                        + "If you didn't request this, you can ignore this email.");
    }

    @Async
    public void sendAssignmentInviteEmail(String to, String invitedByName, String assignmentTitle) {
        send(to, "You've been invited to collaborate on \"" + assignmentTitle + "\"",
                invitedByName + " invited you to collaborated on the assignment \"" + assignmentTitle + "\" on StudentLife.\n\n"
        + "Log in to StudentLife to accept or decline this invite.");
    }

    @Async
    public void sendAccountBannedEmail(String to, String reason) {
        send(to, "Your StudentLife Account has been suspended",
                "your account has been suspended.\n\n"
        + "Reason: " + reason + "\n\n"
        + "if you believe this is a mistake, Please contact support.");
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to sent email to {}: {}", to, e.getMessage());
        }
    }
}
