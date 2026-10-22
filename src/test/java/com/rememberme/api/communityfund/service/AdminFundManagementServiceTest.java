package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.*;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.entity.*;
import com.rememberme.api.communityfund.enums.*;
import com.rememberme.api.communityfund.repository.*;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.impl.AdminFundManagementServiceImpl;
import com.rememberme.api.entity.*;
import com.rememberme.api.entity.User.Role;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.AuditLogRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.repository.FundLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminFundManagementServiceTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportDocumentRepository supportDocumentRepository;

    @Mock
    private SupportRequestUpdateRepository updateRepository;

    @Mock
    private SupportInformationRequestRepository infoRequestRepository;

    @Mock
    private SupportApprovalRepository approvalRepository;

    @Mock
    private FundReservationRepository reservationRepository;

    @Mock
    private ProviderPaymentRepository providerPaymentRepository;

    @Mock
    private FundRefundRepository refundRepository;

    @Mock
    private FundLedgerRepository fundLedgerRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CommunityFundSecurityHelper securityHelper;

    @InjectMocks
    private AdminFundManagementServiceImpl adminFundService;

    private User adminUser1;
    private User adminUser2;
    private User regularUser;
    private SupportRequest sampleRequest;

    @BeforeEach
    void setUp() {
        adminUser1 = new User();
        adminUser1.setId(101L);
        adminUser1.setEmail("admin1@rememberme.com");
        adminUser1.setRole(Role.ADMIN);

        adminUser2 = new User();
        adminUser2.setId(102L);
        adminUser2.setEmail("admin2@rememberme.com");
        adminUser2.setRole(Role.ADMIN);

        regularUser = new User();
        regularUser.setId(201L);
        regularUser.setEmail("user@example.com");
        regularUser.setRole(Role.USER);

        sampleRequest = SupportRequest.builder()
                .id(1L)
                .applicant(regularUser)
                .applicantName("Applicant Name")
                .email("applicant@example.com")
                .phone("+447700900123")
                .relationshipToDeceased("Son")
                .authorityToApply(true)
                .burialProviderName("London Burial Services")
                .burialProviderContact("+447700900456")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.HIGH)
                .supportingNotes("Urgent assistance required")
                .status(SupportRequestStatus.PENDING)
                .providerVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lenient().when(securityHelper.isAdminOrSuperAdmin(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u != null && (u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN);
        });
    }

    // ==========================================
    // 6.1 Admin Review & Verification Tests
    // ==========================================

    @Test
    @DisplayName("Review support request updates status and logs audit")
    void testReviewSupportRequest_Success() {
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminReviewRequestDto reviewDto = AdminReviewRequestDto.builder()
                .status(SupportRequestStatus.UNDER_REVIEW)
                .reviewNotes("Initial review complete")
                .build();

        SupportRequestResponseDto response = adminFundService.reviewSupportRequest(1L, reviewDto, adminUser1);

        assertNotNull(response);
        assertEquals(SupportRequestStatus.UNDER_REVIEW, response.getStatus());
        verify(updateRepository, times(1)).save(any(SupportRequestUpdate.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Request additional info moves status to MORE_INFO_REQUIRED")
    void testRequestAdditionalInfo_Success() {
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminRequestInfoDto infoDto = AdminRequestInfoDto.builder()
                .message("Please upload the formal cemetery plot invoice")
                .requiredDocumentTypes(List.of("INVOICE"))
                .build();

        SupportRequestResponseDto response = adminFundService.requestAdditionalInfo(1L, infoDto, adminUser1);

        assertNotNull(response);
        assertEquals(SupportRequestStatus.MORE_INFO_REQUIRED, response.getStatus());
        verify(infoRequestRepository, times(1)).save(any(SupportInformationRequest.class));
        verify(updateRepository, times(1)).save(any(SupportRequestUpdate.class));
    }

    @Test
    @DisplayName("Verify burial provider sets verified flag and notes")
    void testVerifyProvider_Success() {
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminProviderVerificationDto dto = AdminProviderVerificationDto.builder()
                .verified(true)
                .verificationNotes("Provider phone and registration confirmed")
                .build();

        SupportRequestResponseDto response = adminFundService.verifyProvider(1L, dto, adminUser1);

        assertNotNull(response);
        assertTrue(response.getProviderVerified());
    }

    @Test
    @DisplayName("Decline support request updates status to DECLINED and records audit reason")
    void testDeclineSupportRequest_Success() {
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminDeclineRequestDto declineDto = AdminDeclineRequestDto.builder()
                .reason("Applicant does not meet community residency criteria")
                .build();

        SupportRequestResponseDto response = adminFundService.declineSupportRequest(1L, declineDto, adminUser1);

        assertNotNull(response);
        assertEquals(SupportRequestStatus.DECLINED, response.getStatus());
        verify(updateRepository, times(1)).save(any(SupportRequestUpdate.class));
    }

    // ==========================================
    // 6.2 Two-Person Approval Tests
    // ==========================================

    @Test
    @DisplayName("First approval records decision but keeps status UNDER_REVIEW")
    void testSubmitApproval_FirstApprover_Success() {
        sampleRequest.setStatus(SupportRequestStatus.UNDER_REVIEW);
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(approvalRepository.findBySupportRequestIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(approvalRepository.existsBySupportRequestIdAndApproverId(1L, adminUser1.getId())).thenReturn(false);
        when(approvalRepository.save(any(SupportApproval.class))).thenAnswer(inv -> {
            SupportApproval a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        SubmitApprovalDto dto = SubmitApprovalDto.builder()
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .reason("Verified expenses within policy limits")
                .build();

        SupportApprovalResponseDto response = adminFundService.submitApproval(1L, dto, adminUser1);

        assertNotNull(response);
        assertEquals(ApprovalDecision.APPROVED, response.getDecision());
        assertEquals(new BigDecimal("1500.00"), response.getApprovedAmount());
    }

    @Test
    @DisplayName("Same admin cannot approve the same request twice")
    void testSubmitApproval_DuplicateApprover_ThrowsConflict() {
        sampleRequest.setStatus(SupportRequestStatus.UNDER_REVIEW);
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(approvalRepository.existsBySupportRequestIdAndApproverId(1L, adminUser1.getId())).thenReturn(true);

        SubmitApprovalDto dto = SubmitApprovalDto.builder()
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .reason("Second attempt by same person")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                adminFundService.submitApproval(1L, dto, adminUser1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already submitted an approval decision"));
    }

    @Test
    @DisplayName("Second approver with matching approved amount transitions request to APPROVED")
    void testSubmitApproval_SecondApprover_FullyApproved() {
        sampleRequest.setStatus(SupportRequestStatus.UNDER_REVIEW);
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(approvalRepository.existsBySupportRequestIdAndApproverId(1L, adminUser2.getId())).thenReturn(false);

        SupportApproval firstApproval = SupportApproval.builder()
                .id(10L)
                .supportRequest(sampleRequest)
                .approver(adminUser1)
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .decisionReason("First check")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(approvalRepository.findBySupportRequestIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(firstApproval));
        when(approvalRepository.save(any(SupportApproval.class))).thenAnswer(inv -> {
            SupportApproval a = inv.getArgument(0);
            a.setId(11L);
            return a;
        });

        SubmitApprovalDto dto = SubmitApprovalDto.builder()
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .reason("Second independent verification complete")
                .build();

        SupportApprovalResponseDto response = adminFundService.submitApproval(1L, dto, adminUser2);

        assertNotNull(response);
        assertEquals(SupportRequestStatus.APPROVED, sampleRequest.getStatus());
        assertEquals(new BigDecimal("1500.00"), sampleRequest.getRequestedAmount());
        verify(supportRequestRepository, times(1)).save(sampleRequest);
    }

    @Test
    @DisplayName("Second approver with conflicting amount throws BAD_REQUEST")
    void testSubmitApproval_ConflictingAmount_ThrowsConflict() {
        sampleRequest.setStatus(SupportRequestStatus.UNDER_REVIEW);
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(approvalRepository.existsBySupportRequestIdAndApproverId(1L, adminUser2.getId())).thenReturn(false);

        SupportApproval firstApproval = SupportApproval.builder()
                .id(10L)
                .supportRequest(sampleRequest)
                .approver(adminUser1)
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .decisionReason("Approved 1500")
                .build();

        when(approvalRepository.findBySupportRequestIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(firstApproval));

        SubmitApprovalDto dto = SubmitApprovalDto.builder()
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1000.00")) // Conflict!
                .reason("I think only 1000")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                adminFundService.submitApproval(1L, dto, adminUser2));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not match the first approver's amount"));
    }

    // ==========================================
    // 6.3 Fund Reservation Tests
    // ==========================================

    @Test
    @DisplayName("Reserve funds succeeds when request is APPROVED and balance is sufficient")
    void testReserveFunds_Success() {
        sampleRequest.setStatus(SupportRequestStatus.APPROVED);

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(reservationRepository.existsBySupportRequestIdAndReservationStatus(1L, FundReservationStatus.RESERVED))
                .thenReturn(false);

        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(1000000L); // 10,000 GBP
        when(reservationRepository.sumActiveReservationsPence()).thenReturn(100000L); // 1,000 GBP

        when(reservationRepository.save(any(FundReservation.class))).thenAnswer(inv -> {
            FundReservation r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReserveFundsDto reserveDto = ReserveFundsDto.builder()
                .amount(new BigDecimal("1500.00"))
                .reason("Burial assistance reserve")
                .build();

        FundReservationResponseDto response = adminFundService.reserveFunds(1L, reserveDto, adminUser1);

        assertNotNull(response);
        assertEquals(FundReservationStatus.RESERVED, response.getReservationStatus());
        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertEquals(SupportRequestStatus.PAYMENT_PENDING, sampleRequest.getStatus());
    }

    @Test
    @DisplayName("Reserve funds fails when available balance is insufficient")
    void testReserveFunds_InsufficientBalance_ThrowsConflict() {
        sampleRequest.setStatus(SupportRequestStatus.APPROVED);

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(reservationRepository.existsBySupportRequestIdAndReservationStatus(1L, FundReservationStatus.RESERVED))
                .thenReturn(false);

        // Only 500 GBP in fund
        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(50000L);
        when(reservationRepository.sumActiveReservationsPence()).thenReturn(0L);

        ReserveFundsDto reserveDto = ReserveFundsDto.builder()
                .amount(new BigDecimal("1500.00"))
                .reason("Burial assistance reserve")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                adminFundService.reserveFunds(1L, reserveDto, adminUser1));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Insufficient available"));
    }

    // ==========================================
    // 6.4 Provider Payment Tests
    // ==========================================

    @Test
    @DisplayName("Initiate provider payment succeeds with active reservation and verified provider")
    void testInitiateProviderPayment_Success() {
        sampleRequest.setStatus(SupportRequestStatus.PAYMENT_PENDING);
        sampleRequest.setProviderVerified(true);

        FundReservation reservation = FundReservation.builder()
                .id(50L)
                .supportRequest(sampleRequest)
                .amount(new BigDecimal("1500.00"))
                .amountPence(150000L)
                .reservationStatus(FundReservationStatus.RESERVED)
                .reservedBy(adminUser1)
                .createdAt(LocalDateTime.now())
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(approvalRepository.countBySupportRequestIdAndDecision(1L, ApprovalDecision.APPROVED)).thenReturn(2L);
        when(providerPaymentRepository.existsByPaymentReference("PAY-REF-001")).thenReturn(false);

        when(providerPaymentRepository.save(any(ProviderPayment.class))).thenAnswer(inv -> {
            ProviderPayment p = inv.getArgument(0);
            p.setId(88L);
            return p;
        });

        InitiateProviderPaymentDto dto = InitiateProviderPaymentDto.builder()
                .supportRequestId(1L)
                .reservationId(50L)
                .amount(new BigDecimal("1500.00"))
                .currency("GBP")
                .paymentReference("PAY-REF-001")
                .build();

        ProviderPaymentResponseDto response = adminFundService.initiateProviderPayment(dto, adminUser1);

        assertNotNull(response);
        assertEquals("PAY-REF-001", response.getPaymentReference());
        assertEquals(ProviderPaymentStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Confirm provider payment settlement marks PAID, updates reservation and posts to ledger")
    void testConfirmProviderPayment_Success() {
        sampleRequest.setStatus(SupportRequestStatus.PAYMENT_PENDING);

        FundReservation reservation = FundReservation.builder()
                .id(50L)
                .supportRequest(sampleRequest)
                .amount(new BigDecimal("1500.00"))
                .amountPence(150000L)
                .reservationStatus(FundReservationStatus.RESERVED)
                .build();

        ProviderPayment payment = ProviderPayment.builder()
                .id(88L)
                .supportRequest(sampleRequest)
                .reservation(reservation)
                .providerName("London Burial Services")
                .amount(new BigDecimal("1500.00"))
                .amountPence(150000L)
                .currency("GBP")
                .paymentReference("PAY-REF-001")
                .status(ProviderPaymentStatus.PENDING)
                .initiatedBy(adminUser1)
                .createdAt(LocalDateTime.now())
                .build();

        when(providerPaymentRepository.findById(88L)).thenReturn(Optional.of(payment));
        when(providerPaymentRepository.save(any(ProviderPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(500000L);
        when(fundLedgerRepository.save(any(FundLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        ProviderPaymentResponseDto response = adminFundService.confirmProviderPayment(88L, adminUser1);

        assertNotNull(response);
        assertEquals(ProviderPaymentStatus.PAID, response.getStatus());
        assertEquals(FundReservationStatus.DISBURSED, reservation.getReservationStatus());
        assertEquals(SupportRequestStatus.PAID, sampleRequest.getStatus());

        verify(fundLedgerRepository, times(1)).save(any(FundLedger.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ==========================================
    // 6.5 Refund Tests
    // ==========================================

    @Test
    @DisplayName("Submit refund validates donation and posts DONATION_REFUND to ledger")
    void testSubmitRefund_Success() {
        Donation donation = Donation.builder()
                .id(77L)
                .amountPence(5000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .feePence(100L)
                .netAmountPence(4900L)
                .refundedAmountPence(0L)
                .stripePaymentIntentId("pi_test_123")
                .build();

        when(donationRepository.findById(77L)).thenReturn(Optional.of(donation));

        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(500000L);
        when(fundLedgerRepository.save(any(FundLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        when(refundRepository.save(any(FundRefund.class))).thenAnswer(inv -> {
            FundRefund r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        SubmitRefundDto refundDto = SubmitRefundDto.builder()
                .originalTransactionId(77L)
                .amount(new BigDecimal("50.00"))
                .reason("Accidental duplicate donation")
                .build();

        RefundResponseDto response = adminFundService.submitRefund(refundDto, adminUser1);

        assertNotNull(response);
        assertEquals(new BigDecimal("50.00"), response.getAmount());
        assertEquals("COMPLETED", response.getStatus());
        verify(fundLedgerRepository, times(1)).save(any(FundLedger.class));
    }

    // ==========================================
    // 6.6 Fund Balance & Reporting Tests
    // ==========================================

    @Test
    @DisplayName("Get fund balance accurately calculates ledger amounts and active reservations")
    void testGetFundBalance() {
        when(fundLedgerRepository.sumSettledDonationGrossPence()).thenReturn(600000L); // 6,000 GBP
        when(fundLedgerRepository.sumTotalFeesPence()).thenReturn(30000L); // 300 GBP
        when(fundLedgerRepository.sumTotalRefundsGrossPence()).thenReturn(0L);
        when(fundLedgerRepository.sumTotalDisbursementsPence()).thenReturn(70000L); // 700 GBP
        when(reservationRepository.sumActiveReservationsPence()).thenReturn(150000L); // 1,500 GBP
        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(500000L); // 5,000 GBP

        FundBalanceResponseDto balance = adminFundService.getFundBalance();

        assertNotNull(balance);
        assertEquals(new BigDecimal("6000.00"), balance.getTotalDonationsReceived());
        assertEquals(new BigDecimal("1500.00"), balance.getActiveReservations());
        assertEquals(new BigDecimal("3500.00"), balance.getAvailableFundBalance());
    }

    @Test
    @DisplayName("Export CSV generates valid comma-separated format")
    void testExportFundReportCsv() {
        when(fundLedgerRepository.sumSettledDonationGrossPence()).thenReturn(600000L);
        when(fundLedgerRepository.sumTotalFeesPence()).thenReturn(30000L);
        when(fundLedgerRepository.sumTotalRefundsGrossPence()).thenReturn(0L);
        when(fundLedgerRepository.sumTotalDisbursementsPence()).thenReturn(70000L);
        when(reservationRepository.sumActiveReservationsPence()).thenReturn(150000L);
        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(500000L);
        when(fundLedgerRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        String csv = adminFundService.exportFundReportCsv();

        assertNotNull(csv);
        assertTrue(csv.contains("COMMUNITY BURIAL FUND - FINANCIAL REPORT"));
        assertTrue(csv.contains("Total Donations Settled"));
        assertTrue(csv.contains("Available Unreserved Balance"));
    }
}
