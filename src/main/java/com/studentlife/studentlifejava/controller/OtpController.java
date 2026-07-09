package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.OtpRequest;
import com.studentlife.studentlifejava.dto.request.OtpVerificationRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.ResetTokenResponse;
import com.studentlife.studentlifejava.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OtpController {

    private final VerificationService verificationService;

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequest request) {
        verificationService.generateAndSaveOtp(request.getEmail());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "OTP sent. It expires in 5 minutes.")
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
