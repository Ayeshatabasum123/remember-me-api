package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.request.InitiateProviderPaymentDto;
import com.rememberme.api.communityfund.dto.request.ReleaseReservationDto;
import com.rememberme.api.communityfund.dto.request.SubmitRefundDto;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.enums.FundReservationStatus;
import com.rememberme.api.communityfund.enums.ProviderPaymentStatus;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.AdminFundManagementService;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.FundLedgerEntryType;
import com.rememberme.api.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund - Admin & Fund Management", description = "Administrative endpoints for fund reservations, provider payments, refunds, financial ledger, and audit history")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminFundController {

    private final AdminFundManagementService adminFundService;
    private final CommunityFundSecurityHelper securityHelper;

    // ==========================================
    // 6.3 Fund Reservation APIs
    // ==========================================

    @GetMapping("/fund/reservations")
    @Operation(summary = "List Fund Reservations", description = "Returns paginated list of fund reservations with optional status filter.")
    public ResponseEntity<ApiResponse<Page<FundReservationResponseDto>>> listReservations(
            @Parameter(description = "Filter by reservation status (ACTIVE, CONSUMED, RELEASED, CANCELLED)") @RequestParam(required = false) FundReservationStatus status,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        Page<FundReservationResponseDto> response = adminFundService.listReservations(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Fund reservations retrieved successfully", response));
    }

    @PostMapping("/fund/reservations/{id}/release")
    @Operation(summary = "Release a Reservation", description = "Releases an active fund reservation back to the available Community Burial Fund pool.")
    public ResponseEntity<ApiResponse<FundReservationResponseDto>> releaseReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseReservationDto dto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        FundReservationResponseDto response = adminFundService.releaseReservation(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success("Fund reservation released successfully", response));
    }

    // ==========================================
    // 6.4 Burial Provider Payment APIs
    // ==========================================

    @PostMapping("/provider-payments")
    @Operation(
            summary = "Initiate Provider Payment",
            description = "Initiates or records an authorized burial provider disbursement against an approved support request and active fund reservation."
    )
    public ResponseEntity<ApiResponse<ProviderPaymentResponseDto>> initiateProviderPayment(
            @Valid @RequestBody InitiateProviderPaymentDto dto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        ProviderPaymentResponseDto response = adminFundService.initiateProviderPayment(dto, admin);
        return ResponseEntity.ok(ApiResponse.success("Provider payment initiated successfully", response));
    }

    @PostMapping("/provider-payments/{id}/confirm")
    @Operation(
            summary = "Confirm Provider Payment Settlement",
            description = "Confirms settlement of a burial provider payment, posts a BURIAL_SUPPORT_DISBURSEMENT entry to the fund ledger, and marks the support request as PAID."
    )
    public ResponseEntity<ApiResponse<ProviderPaymentResponseDto>> confirmProviderPayment(
            @PathVariable Long id) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        ProviderPaymentResponseDto response = adminFundService.confirmProviderPayment(id, admin);
        return ResponseEntity.ok(ApiResponse.success("Provider payment confirmed and ledger posted successfully", response));
    }

    @GetMapping("/provider-payments")
    @Operation(summary = "List Provider Payments", description = "Returns paginated list of burial provider payments with optional status filter.")
    public ResponseEntity<ApiResponse<Page<ProviderPaymentResponseDto>>> listProviderPayments(
            @Parameter(description = "Filter by payment status (INITIATED, PROCESSING, PAID, FAILED, CANCELLED)") @RequestParam(required = false) ProviderPaymentStatus status,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        Page<ProviderPaymentResponseDto> response = adminFundService.listProviderPayments(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Provider payments retrieved successfully", response));
    }

    @GetMapping("/provider-payments/{id}")
    @Operation(summary = "Get Provider Payment Details", description = "Retrieves complete record of a burial provider payment.")
    public ResponseEntity<ApiResponse<ProviderPaymentResponseDto>> getProviderPayment(@PathVariable Long id) {
        ProviderPaymentResponseDto response = adminFundService.getProviderPayment(id);
        return ResponseEntity.ok(ApiResponse.success("Provider payment details retrieved successfully", response));
    }

    // ==========================================
    // 6.5 Refund and Reversal APIs
    // ==========================================

    @PostMapping("/refunds")
    @Operation(
            summary = "Request and Process Refund",
            description = "Processes a refund for a previous donation transaction. Validates refundable balance, posts DONATION_REFUND to the ledger, and records full audit trail."
    )
    public ResponseEntity<ApiResponse<RefundResponseDto>> submitRefund(
            @Valid @RequestBody SubmitRefundDto dto) {
        User admin = securityHelper.getCurrentAuthenticatedUser();
        RefundResponseDto response = adminFundService.submitRefund(dto, admin);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    @GetMapping("/refunds")
    @Operation(summary = "View Refunds", description = "Returns paginated list of all processed refunds.")
    public ResponseEntity<ApiResponse<Page<RefundResponseDto>>> listRefunds(
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        Page<RefundResponseDto> response = adminFundService.listRefunds(page, size);
        return ResponseEntity.ok(ApiResponse.success("Refunds retrieved successfully", response));
    }

    @GetMapping("/refunds/{id}")
    @Operation(summary = "View Refund Details", description = "Retrieves details of a specific refund record.")
    public ResponseEntity<ApiResponse<RefundResponseDto>> getRefund(@PathVariable Long id) {
        RefundResponseDto response = adminFundService.getRefund(id);
        return ResponseEntity.ok(ApiResponse.success("Refund details retrieved successfully", response));
    }

    // ==========================================
    // 6.6 Fund Accounts, Ledger & Reporting APIs
    // ==========================================

    @GetMapping("/fund/ledger")
    @Operation(summary = "View Financial Transactions Ledger", description = "Returns double-entry fund transactions ledger with running balances.")
    public ResponseEntity<ApiResponse<Page<FundLedgerEntryDto>>> getFundLedger(
            @Parameter(description = "Filter by transaction entry type") @RequestParam(required = false) FundLedgerEntryType entryType,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        Page<FundLedgerEntryDto> response = adminFundService.getFundLedger(entryType, page, size);
        return ResponseEntity.ok(ApiResponse.success("Fund ledger retrieved successfully", response));
    }

    @GetMapping("/fund/balance")
    @Operation(summary = "Get Fund Balance Summary", description = "Returns total gross, net, fee, refund, disbursement, reserved, and available fund balances.")
    public ResponseEntity<ApiResponse<FundBalanceResponseDto>> getFundBalance() {
        FundBalanceResponseDto response = adminFundService.getFundBalance();
        return ResponseEntity.ok(ApiResponse.success("Fund balance retrieved successfully", response));
    }

    @GetMapping("/fund/reports")
    @Operation(summary = "Get Financial Reports", description = "Summarizes financial inflows, outflows, reservations, and case counts.")
    public ResponseEntity<ApiResponse<FundReportResponseDto>> getFundReport() {
        FundReportResponseDto response = adminFundService.getFundReport();
        return ResponseEntity.ok(ApiResponse.success("Financial report retrieved successfully", response));
    }

    @GetMapping(value = "/fund/reports/export", produces = "text/csv")
    @Operation(summary = "Export Financial Report (CSV)", description = "Exports financial accounting summary in CSV format for audit and compliance.")
    public ResponseEntity<byte[]> exportFundReport() {
        String csv = adminFundService.exportFundReportCsv();
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"burial_fund_financial_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/fund/impact")
    @Operation(summary = "Get Community Fund Impact Metrics", description = "Returns aggregate impact figures including cases supported, total disbursed, and average assistance per case.")
    public ResponseEntity<ApiResponse<FundImpactResponseDto>> getFundImpact() {
        FundImpactResponseDto response = adminFundService.getFundImpact();
        return ResponseEntity.ok(ApiResponse.success("Fund impact metrics retrieved successfully", response));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View Administrative Audit Logs", description = "Returns paginated audit trail of administrative and financial operations.")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDto>>> getAuditLogs(
            @Parameter(description = "Filter by entity type (e.g., SupportRequest, FundReservation, ProviderPayment, FundRefund)") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by action keyword") @RequestParam(required = false) String action,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        Page<AuditLogResponseDto> response = adminFundService.getAuditLogs(entityType, action, page, size);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", response));
    }
}
