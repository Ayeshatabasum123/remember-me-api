package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.request.*;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.AdminFundManagementService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund - Admin Request Management", description = "Administrative endpoints for application review, provider verification, two-person approval, and fund reservations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSupportRequestController {

    private final AdminFundManagementService adminFundService;
    private final CommunityFundSecurityHelper securityHelper;

    @GetMapping
    @Operation(summary = "List and Filter Support Requests", description = "Admin dashboard list of burial support applications with status, urgency, and keyword filtering. Supports pagination.")
    public ResponseEntity<ApiResponse<Page<SupportRequestResponseDto>>> listRequests(
            @Parameter(description = "Filter by status") @RequestParam(required = false) SupportRequestStatus status,
            @Parameter(description = "Filter by urgency") @RequestParam(required = false) SupportUrgency urgency,
            @Parameter(description = "Search keyword (applicant name, provider, email)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        Page<SupportRequestResponseDto> response = adminFundService.getAdminSupportRequests(status, urgency, search, page, size);
        return ResponseEntity.ok(ApiResponse.success("Support requests retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Support Request Application Details", description = "Fetches complete application record including financial amounts, costs, and audit metadata.")
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> getRequestDetails(@PathVariable Long id) {
        SupportRequestResponseDto response = adminFundService.getAdminSupportRequestDetails(id);
        return ResponseEntity.ok(ApiResponse.success("Support request details retrieved successfully", response));
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Review Supporting Documents", description = "List all documents and metadata uploaded in support of this application.")
    public ResponseEntity<ApiResponse<List<SupportDocumentResponseDto>>> getRequestDocuments(@PathVariable Long id) {
        List<SupportDocumentResponseDto> response = adminFundService.getAdminSupportRequestDocuments(id);
        return ResponseEntity.ok(ApiResponse.success("Support documents retrieved successfully", response));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "Update Application Review", description = "Updates review notes and application lifecycle status.")
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> reviewRequest(
            @PathVariable Long id,
            @Valid @RequestBody AdminReviewRequestDto reviewDto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = adminFundService.reviewSupportRequest(id, reviewDto, admin);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @PostMapping("/{id}/request-info")
    @Operation(summary = "Request Additional Information", description = "Requests more documentation or explanation from the applicant. Moves application to MORE_INFO_REQUIRED.")
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> requestInfo(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequestInfoDto requestInfoDto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = adminFundService.requestAdditionalInfo(id, requestInfoDto, admin);
        return ResponseEntity.ok(ApiResponse.success("Additional information requested successfully", response));
    }

    @PutMapping("/{id}/provider-verification")
    @Operation(summary = "Verify Burial Provider", description = "Records verified status of funeral director or burial service provider.")
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> verifyProvider(
            @PathVariable Long id,
            @Valid @RequestBody AdminProviderVerificationDto verificationDto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = adminFundService.verifyProvider(id, verificationDto, admin);
        return ResponseEntity.ok(ApiResponse.success("Burial provider verification updated successfully", response));
    }

    @PutMapping("/{id}/decline")
    @Operation(summary = "Decline Support Request", description = "Declines financial assistance application with formal audit reason.")
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> declineRequest(
            @PathVariable Long id,
            @Valid @RequestBody AdminDeclineRequestDto declineDto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = adminFundService.declineSupportRequest(id, declineDto, admin);
        return ResponseEntity.ok(ApiResponse.success("Application declined", response));
    }

    // ==========================================
    // Two-Person Approval
    // ==========================================

    @PostMapping("/{id}/approvals")
    @Operation(
            summary = "Submit Approval Decision",
            description = "Submit one of two required administrative approval decisions. Enforces two distinct approvers, matching authorized amounts, and automatic APPROVED status transition upon second matching approval."
    )
    public ResponseEntity<ApiResponse<SupportApprovalResponseDto>> submitApproval(
            @PathVariable Long id,
            @Valid @RequestBody SubmitApprovalDto approvalDto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        SupportApprovalResponseDto response = adminFundService.submitApproval(id, approvalDto, admin);
        return ResponseEntity.ok(ApiResponse.success("Approval decision recorded successfully", response));
    }

    @GetMapping("/{id}/approvals")
    @Operation(summary = "Get Approval Records", description = "Returns chronological two-person approval records for this application.")
    public ResponseEntity<ApiResponse<List<SupportApprovalResponseDto>>> getApprovals(@PathVariable Long id) {
        List<SupportApprovalResponseDto> response = adminFundService.getRequestApprovals(id);
        return ResponseEntity.ok(ApiResponse.success("Approvals retrieved successfully", response));
    }

    // ==========================================
    // Fund Reservation
    // ==========================================

    @PostMapping("/{id}/reserve-funds")
    @Operation(
            summary = "Reserve Funds",
            description = "Reserves funds from the shared Community Burial Fund following two-person approval. Verifies fund availability and prevents duplicate reservations."
    )
    public ResponseEntity<ApiResponse<FundReservationResponseDto>> reserveFunds(
            @PathVariable Long id,
            @Valid @RequestBody ReserveFundsDto reserveDto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        FundReservationResponseDto response = adminFundService.reserveFunds(id, reserveDto, admin);
        return ResponseEntity.ok(ApiResponse.success("Funds reserved successfully", response));
    }
}
