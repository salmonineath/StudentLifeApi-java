package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.request.OtpRequest;
import com.studentlife.studentlifejava.dto.request.OtpVerificationRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.otp.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequest request) {
        otpService.generateAndSaveOtp(request.getEmail());

        return ResponseEntity.ok(new ApiResponse<>(200, true, "OTP sent. It expires in 5 minutes."));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        boolean isVerified = otpService.validateAndDestroyOtp(request.getEmail(), request.getOtp());

        if (isVerified) {
            return ResponseEntity.ok(new ApiResponse<>(200, true, "OTP verified successfully."));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(401, false, "Invalid, expired, or already used OTP."));
    }
}
