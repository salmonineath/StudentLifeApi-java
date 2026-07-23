package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.OtpRequest;
import com.studentlife.studentlifejava.dto.request.OtpVerificationRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.ResetTokenResponse;
import com.studentlife.studentlifejava.service.VerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Password-reset OTP flow — part of the authentication lifecycle")
public class OtpController {

    private final VerificationService verificationService;

    // Kept in sync with app.security.otp.validity-minutes so this message never
    // drifts out of sync with the actual TTL configured for VerificationServiceImpl.
    @Value("${app.security.otp.validity-minutes}")
    private long otpValidityMinutes;

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequest request) {
        verificationService.generateAndSaveOtp(request.getEmail());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "OTP sent. It expires in " + otpValidityMinutes + " minutes.")
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request
    ) {
        boolean isVerified = verificationService.validateAndDestroyOtp(
                request.getEmail(), request.getOtp()
        );

        if (isVerified) {

            String resetToken = verificationService.generateResetToken(request.getEmail());

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            200,
                            true,
                            "OTP verified successfully.",
                            new ResetTokenResponse(resetToken)
                    )
            );
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ApiResponse<>(
                                401,
                                false,
                                "Invalid, expired, or already used OTP."
                        )
                );
    }
}
