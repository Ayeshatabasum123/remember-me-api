package com.rememberme.api.service;

import com.rememberme.api.dto.request.ForgotPasswordRequest;
import com.rememberme.api.dto.request.LoginRequest;
import com.rememberme.api.dto.request.RegisterRequest;
import com.rememberme.api.dto.request.ResetPasswordRequest;
import com.rememberme.api.dto.request.SendOtpRequest;
import com.rememberme.api.dto.request.VerifyOtpRequest;
import com.rememberme.api.dto.response.AuthResponse;
import com.rememberme.api.entity.OtpVerification;
import com.rememberme.api.entity.PasswordResetToken;
import com.rememberme.api.entity.TokenBlacklist;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.OtpVerificationRepository;
import com.rememberme.api.repository.PasswordResetTokenRepository;
import com.rememberme.api.repository.TokenBlacklistRepository;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email already registered", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .enabled(true)
                .emailVerified(false)
                .phoneVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException("Missing or invalid authorization header", HttpStatus.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            throw new ApiException("Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
        if (tokenBlacklistRepository.existsByToken(token)) {
            throw new ApiException("Token is already invalidated / logged out", HttpStatus.UNAUTHORIZED);
        }

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .token(token)
                .expiryTime(LocalDateTime.now().plusHours(24))
                .build();
        tokenBlacklistRepository.save(blacklist);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Email not found", HttpStatus.NOT_FOUND));

        String token = java.util.UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(user.getEmail())
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = "http://localhost:8080/api/auth/reset-password?token=" + token;
        String emailBody = "Hello " + user.getFullName() + ",\n\n" +
                "You requested a password reset. Please use the following token to reset your password:\n" +
                token + "\n\n" +
                "Or click the link below:\n" +
                resetLink + "\n\n" +
                "This token will expire in 15 minutes.\n\n" +
                "Regards,\nRemember Me Team";

        emailService.sendEmail(user.getEmail(), "Password Reset Request", emailBody);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ApiException("Invalid reset token", HttpStatus.BAD_REQUEST));

        if (resetToken.isUsed()) {
            throw new ApiException("Reset token has already been used", HttpStatus.BAD_REQUEST);
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException("Reset token has expired", HttpStatus.GONE);
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ApiException("User not found for this token", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    public void sendOtp(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new ApiException("Email is already verified", HttpStatus.BAD_REQUEST);
        }

        Optional<OtpVerification> existingOtpOpt = otpVerificationRepository.findByEmail(request.getEmail());
        OtpVerification otpVerification;
        LocalDateTime now = LocalDateTime.now();

        String generatedOtp = String.format("%06d", new Random().nextInt(1000000));

        if (existingOtpOpt.isPresent()) {
            otpVerification = existingOtpOpt.get();

            if (otpVerification.getLastSentAt().plusMinutes(1).isAfter(now)) {
                throw new ApiException("Please wait before requesting a new OTP (cooldown active)", HttpStatus.TOO_MANY_REQUESTS);
            }

            if (otpVerification.getResendAttempts() >= 5) {
                if (otpVerification.getLastSentAt().plusHours(1).isAfter(now)) {
                    throw new ApiException("Max resend attempts exceeded. Please try again after an hour.", HttpStatus.TOO_MANY_REQUESTS);
                } else {
                    otpVerification.setResendAttempts(0);
                }
            }

            otpVerification.setOtp(generatedOtp);
            otpVerification.setExpiryTime(now.plusMinutes(5));
            otpVerification.setLastSentAt(now);
            otpVerification.setResendAttempts(otpVerification.getResendAttempts() + 1);
        } else {
            otpVerification = OtpVerification.builder()
                    .email(request.getEmail())
                    .otp(generatedOtp)
                    .expiryTime(now.plusMinutes(5))
                    .lastSentAt(now)
                    .resendAttempts(1)
                    .build();
        }

        otpVerificationRepository.save(otpVerification);

        String emailBody = "Hello " + user.getFullName() + ",\n\n" +
                "Your OTP code for verification is: " + generatedOtp + "\n\n" +
                "This OTP will expire in 5 minutes.\n\n" +
                "Regards,\nRemember Me Team";

        emailService.sendEmail(user.getEmail(), "Email Verification OTP", emailBody);
    }

    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new ApiException("Email is already verified", HttpStatus.BAD_REQUEST);
        }

        OtpVerification otpVerification = otpVerificationRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("No OTP requested for this email", HttpStatus.BAD_REQUEST));

        if (otpVerification.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException("OTP has expired", HttpStatus.GONE);
        }

        if (!otpVerification.getOtp().equals(request.getOtp())) {
            throw new ApiException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }

        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        otpVerificationRepository.delete(otpVerification);
    }
}
