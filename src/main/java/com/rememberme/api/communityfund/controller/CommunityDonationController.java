package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.request.DonationCheckoutRequest;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.CommunityDonationService;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund - Donations", description = "Endpoints for donation preset options, secure checkout initiation, donation status lookup, official receipts, and retry workflows")
public class CommunityDonationController {

    private final CommunityDonationService donationService;
    private final CommunityFundSecurityHelper securityHelper;

    @GetMapping("/amounts")
    @Operation(
            summary = "Get Predefined Donation Amounts",
            description = "Returns predefined donation tiers (£5, £10, £50), custom amount boundaries (£1 to £50,000), currency details, and the shared communal fund policy explanation. Publicly accessible."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Donation amounts and options retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DonationAmountsResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<DonationAmountsResponse>> getDonationAmounts() {
        log.info("Request received for donation amounts and options");
        DonationAmountsResponse response = donationService.getDonationAmounts();
        return ResponseEntity.ok(ApiResponse.success("Donation options retrieved successfully", response));
    }

    @PostMapping("/checkout")
    @Operation(
            summary = "Initiate Donation Checkout",
            description = "Creates a pending donation record, registers optional loved-one tributes, initiates checkout with the payment provider (Stripe), and returns client secret and donation ID. Requires JWT authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Donation checkout session initiated successfully",
                    content = @Content(schema = @Schema(implementation = DonationCheckoutResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed for amount or currency"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token")
    })
    public ResponseEntity<ApiResponse<DonationCheckoutResponse>> createCheckout(
            @Valid @RequestBody DonationCheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        User donor = securityHelper.getCurrentAuthenticatedUser();
        log.info("User ID {} ({}) initiating checkout for £{}", donor.getId(), donor.getEmail(), request.getAmount());
        DonationCheckoutResponse response = donationService.createCheckout(donor, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Donation checkout initiated successfully", response));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get My Donations",
            description = "Returns paginated donation history strictly private to the authenticated donor, newest first by default. Supports page, size, sortBy, and direction parameters. Requires JWT authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User donation history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token")
    })
    public ResponseEntity<ApiResponse<Page<DonationDetailsResponse>>> getMyDonations(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property (createdAt, amount, status)", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)", example = "DESC") @RequestParam(defaultValue = "DESC") String direction) {
        User donor = securityHelper.getCurrentAuthenticatedUser();
        log.info("Retrieving donation history for user ID {} ({}), page={}, size={}", donor.getId(), donor.getEmail(), page, size);
        Page<DonationDetailsResponse> response = donationService.getMyDonations(donor, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success("Donation history retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Donation Details & Status",
            description = "Returns details, payment status, dedication information, and client secret for an existing donation. Restricted strictly to the donor owner or authorized administrators.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Donation details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DonationDetailsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Cannot access another donor's records"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Donation not found")
    })
    public ResponseEntity<ApiResponse<DonationDetailsResponse>> getDonationById(
            @Parameter(description = "Donation ID", example = "101") @PathVariable Long id) {
        User user = securityHelper.getCurrentAuthenticatedUser();
        DonationDetailsResponse response = donationService.getDonationDetails(id, user);
        return ResponseEntity.ok(ApiResponse.success("Donation details retrieved successfully", response));
    }

    @GetMapping("/{id}/receipt")
    @Operation(
            summary = "Get Official Donation Receipt",
            description = "Returns official receipt details (receipt number, donor info, amount, payment method, issued timestamp) for a confirmed, successful donation. Fails if payment is still pending, failed, or incomplete.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Donation receipt retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DonationReceiptInfoResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Receipt unavailable: payment not yet confirmed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Not authorized to view this receipt"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receipt or donation not found")
    })
    public ResponseEntity<ApiResponse<DonationReceiptInfoResponse>> getDonationReceipt(
            @Parameter(description = "Donation ID", example = "101") @PathVariable Long id) {
        User user = securityHelper.getCurrentAuthenticatedUser();
        DonationReceiptInfoResponse response = donationService.getDonationReceipt(id, user);
        return ResponseEntity.ok(ApiResponse.success("Donation receipt retrieved successfully", response));
    }

    @PostMapping("/{id}/retry")
    @Operation(
            summary = "Retry Failed or Expired Donation",
            description = "Allows safe retry of a failed, cancelled, or expired donation payment by refreshing the checkout session with a valid client secret. Rejects retrying already confirmed donations to prevent double charging.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Donation payment retry initiated successfully",
                    content = @Content(schema = @Schema(implementation = DonationRetryResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Donation already completed or invalid state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Cannot retry another user's donation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Donation not found")
    })
    public ResponseEntity<ApiResponse<DonationRetryResponse>> retryDonation(
            @Parameter(description = "Donation ID", example = "101") @PathVariable Long id) {
        User user = securityHelper.getCurrentAuthenticatedUser();
        DonationRetryResponse response = donationService.retryDonation(id, user);
        return ResponseEntity.ok(ApiResponse.success("Donation payment retry initiated successfully", response));
    }
}
