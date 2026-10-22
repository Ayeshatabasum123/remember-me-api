package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.*;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.enums.FundReservationStatus;
import com.rememberme.api.communityfund.enums.ProviderPaymentStatus;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.entity.FundLedgerEntryType;
import com.rememberme.api.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminFundManagementService {

    // Request Review & Verification
    Page<SupportRequestResponseDto> getAdminSupportRequests(SupportRequestStatus status, SupportUrgency urgency, String search, int page, int size);

    SupportRequestResponseDto getAdminSupportRequestDetails(Long id);

    List<SupportDocumentResponseDto> getAdminSupportRequestDocuments(Long id);

    SupportRequestResponseDto reviewSupportRequest(Long id, AdminReviewRequestDto dto, User adminUser);

    SupportRequestResponseDto requestAdditionalInfo(Long id, AdminRequestInfoDto dto, User adminUser);

    SupportRequestResponseDto verifyProvider(Long id, AdminProviderVerificationDto dto, User adminUser);

    SupportRequestResponseDto declineSupportRequest(Long id, AdminDeclineRequestDto dto, User adminUser);

    // Two-Person Approval
    SupportApprovalResponseDto submitApproval(Long requestId, SubmitApprovalDto dto, User approverUser);

    List<SupportApprovalResponseDto> getRequestApprovals(Long requestId);

    // Fund Reservations
    FundReservationResponseDto reserveFunds(Long requestId, ReserveFundsDto dto, User adminUser);

    Page<FundReservationResponseDto> listReservations(FundReservationStatus status, int page, int size);

    FundReservationResponseDto releaseReservation(Long reservationId, ReleaseReservationDto dto, User adminUser);

    // Provider Payments
    ProviderPaymentResponseDto initiateProviderPayment(InitiateProviderPaymentDto dto, User adminUser);

    ProviderPaymentResponseDto confirmProviderPayment(Long paymentId, User adminUser);

    Page<ProviderPaymentResponseDto> listProviderPayments(ProviderPaymentStatus status, int page, int size);

    ProviderPaymentResponseDto getProviderPayment(Long paymentId);

    // Refunds
    RefundResponseDto submitRefund(SubmitRefundDto dto, User adminUser);

    Page<RefundResponseDto> listRefunds(int page, int size);

    RefundResponseDto getRefund(Long refundId);

    // Fund Accounts, Ledger & Reports
    Page<FundLedgerEntryDto> getFundLedger(FundLedgerEntryType entryType, int page, int size);

    FundBalanceResponseDto getFundBalance();

    FundReportResponseDto getFundReport();

    String exportFundReportCsv();

    FundImpactResponseDto getFundImpact();

    Page<AuditLogResponseDto> getAuditLogs(String entityType, String action, int page, int size);
}
