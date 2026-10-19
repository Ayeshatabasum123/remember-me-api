package com.rememberme.api.controller;

import com.rememberme.api.dto.request.CreateDonationPaymentIntentRequest;
import com.rememberme.api.dto.response.*;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.service.DonationReceiptService;
import com.rememberme.api.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund Donations", description = "Endpoints for donations, Stripe PaymentIntents, personal donation history, and receipts")
public class DonationController {

    private final DonationService donationService;
    private final DonationReceiptService donationReceiptService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
    }

    @GetMapping("/api/v1/donations/options")
    @Operation(summary = "Get Donation Options & Presets", description = "Returns preset tiers (£5, £10, £50), permitted custom amount range, GBP currency, and shared-fund explanation", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<DonationOptionsResponse> getDonationOptions() {
        DonationOptionsResponse options = donationService.getDonationOptions();
        return ApiResponse.success("Donation options retrieved successfully", options);
    }

    @PostMapping("/api/v1/donations/payment-intent")
    @Operation(summary = "Create Stripe PaymentIntent", description = "Validates the amount and dedications on the server, creates a pending donation, creates or reuses a Stripe PaymentIntent, and returns donationId and clientSecret for Flutter Stripe PaymentSheet", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CreatePaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody CreateDonationPaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        User donor = getAuthenticatedUser();
        CreatePaymentIntentResponse response = donationService.createPaymentIntent(donor, request, idempotencyKey);
        return ApiResponse.success("Payment intent created successfully", response);
    }

    @GetMapping("/api/v1/me/donations")
    @Operation(summary = "Get My Donation History", description = "Returns paginated donation history strictly private to the authenticated donor, newest first", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<DonationResponse>> getMyDonations(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        User donor = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DonationResponse> response = donationService.getMyDonations(donor, pageable);
        return ApiResponse.success("Donation history retrieved successfully", response);
    }

    @GetMapping("/api/v1/me/donations/{id}")
    @Operation(summary = "Get Donation Status & Details", description = "Returns status and details of a single donation, strictly enforcing donor ownership", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<DonationResponse> getMyDonationById(@PathVariable Long id) {
        User donor = getAuthenticatedUser();
        DonationResponse response = donationService.getMyDonationById(id, donor.getId());
        return ApiResponse.success("Donation details retrieved successfully", response);
    }

    @GetMapping("/api/v1/me/donations/{id}/receipt")
    @Operation(summary = "Get Official Donation Receipt", description = "Returns the official receipt for a donation strictly to its donor", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<DonationReceiptResponse> getMyDonationReceipt(@PathVariable Long id) {
        User donor = getAuthenticatedUser();
        DonationReceiptResponse response = donationReceiptService.getReceiptForDonor(id, donor.getId());
        return ApiResponse.success("Receipt retrieved successfully", response);
    }
}
