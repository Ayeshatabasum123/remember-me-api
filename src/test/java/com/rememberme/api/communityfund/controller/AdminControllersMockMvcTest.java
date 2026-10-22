package com.rememberme.api.communityfund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.communityfund.dto.request.*;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.enums.ApprovalDecision;
import com.rememberme.api.communityfund.enums.FundReservationStatus;
import com.rememberme.api.communityfund.enums.ProviderPaymentStatus;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.AdminFundManagementService;
import com.rememberme.api.entity.User;
import com.rememberme.api.entity.User.Role;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminSupportRequestController.class, AdminFundController.class})
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllersMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminFundManagementService adminFundService;

    @MockBean
    private CommunityFundSecurityHelper securityHelper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    private User adminUser;
    private SupportRequestResponseDto sampleRequestResponse;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(101L);
        adminUser.setEmail("admin@rememberme.com");
        adminUser.setRole(Role.ADMIN);

        sampleRequestResponse = SupportRequestResponseDto.builder()
                .id(1L)
                .applicantUserId(201L)
                .applicantName("Applicant Name")
                .email("applicant@example.com")
                .phone("+447700900123")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.HIGH)
                .status(SupportRequestStatus.PENDING)
                .providerVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(securityHelper.getCurrentAuthenticatedUser()).thenReturn(adminUser);
    }

    // ==========================================
    // Admin Support Request Review Controller Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/admin/support-requests returns paginated requests")
    void testListRequests() throws Exception {
        when(adminFundService.getAdminSupportRequests(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(sampleRequestResponse)));

        mockMvc.perform(get("/api/admin/support-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/admin/support-requests/{id} returns request details")
    void testGetRequestDetails() throws Exception {
        when(adminFundService.getAdminSupportRequestDetails(1L)).thenReturn(sampleRequestResponse);

        mockMvc.perform(get("/api/admin/support-requests/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.currency").value("GBP"));
    }

    @Test
    @DisplayName("PUT /api/admin/support-requests/{id}/review updates review state")
    void testReviewRequest() throws Exception {
        sampleRequestResponse.setStatus(SupportRequestStatus.UNDER_REVIEW);
        when(adminFundService.reviewSupportRequest(eq(1L), any(), any())).thenReturn(sampleRequestResponse);

        AdminReviewRequestDto dto = AdminReviewRequestDto.builder()
                .status(SupportRequestStatus.UNDER_REVIEW)
                .reviewNotes("Verified funeral director details")
                .build();

        mockMvc.perform(put("/api/admin/support-requests/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("POST /api/admin/support-requests/{id}/request-info requests more documents")
    void testRequestInfo() throws Exception {
        sampleRequestResponse.setStatus(SupportRequestStatus.MORE_INFO_REQUIRED);
        when(adminFundService.requestAdditionalInfo(eq(1L), any(), any())).thenReturn(sampleRequestResponse);

        AdminRequestInfoDto dto = AdminRequestInfoDto.builder()
                .message("Please provide updated burial invoice")
                .requiredDocumentTypes(List.of("INVOICE"))
                .build();

        mockMvc.perform(post("/api/admin/support-requests/1/request-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("MORE_INFO_REQUIRED"));
    }

    @Test
    @DisplayName("PUT /api/admin/support-requests/{id}/provider-verification updates provider flag")
    void testVerifyProvider() throws Exception {
        sampleRequestResponse.setProviderVerified(true);
        when(adminFundService.verifyProvider(eq(1L), any(), any())).thenReturn(sampleRequestResponse);

        AdminProviderVerificationDto dto = AdminProviderVerificationDto.builder()
                .verified(true)
                .verificationNotes("Provider verified via official registry")
                .build();

        mockMvc.perform(put("/api/admin/support-requests/1/provider-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerVerified").value(true));
    }

    @Test
    @DisplayName("PUT /api/admin/support-requests/{id}/decline declines application")
    void testDeclineRequest() throws Exception {
        sampleRequestResponse.setStatus(SupportRequestStatus.DECLINED);
        when(adminFundService.declineSupportRequest(eq(1L), any(), any())).thenReturn(sampleRequestResponse);

        AdminDeclineRequestDto dto = AdminDeclineRequestDto.builder()
                .reason("Ineligible funding request")
                .build();

        mockMvc.perform(put("/api/admin/support-requests/1/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }

    @Test
    @DisplayName("POST /api/admin/support-requests/{id}/approvals records approval decision")
    void testSubmitApproval() throws Exception {
        SupportApprovalResponseDto approvalResponse = SupportApprovalResponseDto.builder()
                .id(10L)
                .supportRequestId(1L)
                .approverUserId(adminUser.getId())
                .approverEmail(adminUser.getEmail())
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(adminFundService.submitApproval(eq(1L), any(), any())).thenReturn(approvalResponse);

        SubmitApprovalDto dto = SubmitApprovalDto.builder()
                .decision(ApprovalDecision.APPROVED)
                .approvedAmount(new BigDecimal("1500.00"))
                .reason("Verified funeral director costs")
                .build();

        mockMvc.perform(post("/api/admin/support-requests/1/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.decision").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedAmount").value(1500.00));
    }

    @Test
    @DisplayName("POST /api/admin/support-requests/{id}/reserve-funds reserves fund balance")
    void testReserveFunds() throws Exception {
        FundReservationResponseDto reservationResponse = FundReservationResponseDto.builder()
                .id(50L)
                .supportRequestId(1L)
                .amount(new BigDecimal("1500.00"))
                .reservationStatus(FundReservationStatus.RESERVED)
                .reservedByUserId(adminUser.getId())
                .createdAt(LocalDateTime.now())
                .build();

        when(adminFundService.reserveFunds(eq(1L), any(), any())).thenReturn(reservationResponse);

        ReserveFundsDto dto = ReserveFundsDto.builder()
                .amount(new BigDecimal("1500.00"))
                .reason("Burial grant reservation")
                .build();

        mockMvc.perform(post("/api/admin/support-requests/1/reserve-funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationStatus").value("RESERVED"))
                .andExpect(jsonPath("$.data.amount").value(1500.00));
    }

    // ==========================================
    // Admin Fund Controller Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/admin/fund/reservations lists reservations")
    void testListReservations() throws Exception {
        FundReservationResponseDto res = FundReservationResponseDto.builder()
                .id(50L)
                .supportRequestId(1L)
                .amount(new BigDecimal("1500.00"))
                .reservationStatus(FundReservationStatus.RESERVED)
                .build();

        when(adminFundService.listReservations(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(res)));

        mockMvc.perform(get("/api/admin/fund/reservations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(50));
    }

    @Test
    @DisplayName("POST /api/admin/fund/reservations/{id}/release releases reservation")
    void testReleaseReservation() throws Exception {
        FundReservationResponseDto res = FundReservationResponseDto.builder()
                .id(50L)
                .supportRequestId(1L)
                .amount(new BigDecimal("1500.00"))
                .reservationStatus(FundReservationStatus.RELEASED)
                .releasedAt(LocalDateTime.now())
                .build();

        when(adminFundService.releaseReservation(eq(50L), any(), any())).thenReturn(res);

        ReleaseReservationDto dto = ReleaseReservationDto.builder()
                .reason("Applicant obtained local council grant")
                .build();

        mockMvc.perform(post("/api/admin/fund/reservations/50/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationStatus").value("RELEASED"));
    }

    @Test
    @DisplayName("POST /api/admin/provider-payments initiates provider payment")
    void testInitiateProviderPayment() throws Exception {
        ProviderPaymentResponseDto payResponse = ProviderPaymentResponseDto.builder()
                .id(88L)
                .supportRequestId(1L)
                .reservationId(50L)
                .providerName("London Burial Services")
                .amount(new BigDecimal("1500.00"))
                .currency("GBP")
                .paymentReference("PAY-REF-001")
                .status(ProviderPaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        when(adminFundService.initiateProviderPayment(any(), any())).thenReturn(payResponse);

        InitiateProviderPaymentDto dto = InitiateProviderPaymentDto.builder()
                .supportRequestId(1L)
                .reservationId(50L)
                .amount(new BigDecimal("1500.00"))
                .currency("GBP")
                .paymentReference("PAY-REF-001")
                .build();

        mockMvc.perform(post("/api/admin/provider-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-REF-001"));
    }

    @Test
    @DisplayName("POST /api/admin/provider-payments/{id}/confirm confirms payment settlement")
    void testConfirmProviderPayment() throws Exception {
        ProviderPaymentResponseDto payResponse = ProviderPaymentResponseDto.builder()
                .id(88L)
                .status(ProviderPaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        when(adminFundService.confirmProviderPayment(eq(88L), any())).thenReturn(payResponse);

        mockMvc.perform(post("/api/admin/provider-payments/88/confirm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /api/admin/refunds submits refund")
    void testSubmitRefund() throws Exception {
        RefundResponseDto refundResponse = RefundResponseDto.builder()
                .id(99L)
                .originalTransactionId(77L)
                .amount(new BigDecimal("50.00"))
                .reason("Accidental duplicate donation")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        when(adminFundService.submitRefund(any(), any())).thenReturn(refundResponse);

        SubmitRefundDto dto = SubmitRefundDto.builder()
                .originalTransactionId(77L)
                .amount(new BigDecimal("50.00"))
                .reason("Accidental duplicate donation")
                .build();

        mockMvc.perform(post("/api/admin/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/admin/fund/balance returns balance summary")
    void testGetFundBalance() throws Exception {
        FundBalanceResponseDto balance = FundBalanceResponseDto.builder()
                .totalDonationsReceived(new BigDecimal("5000.00"))
                .activeReservations(new BigDecimal("1500.00"))
                .availableFundBalance(new BigDecimal("3500.00"))
                .currency("GBP")
                .build();

        when(adminFundService.getFundBalance()).thenReturn(balance);

        mockMvc.perform(get("/api/admin/fund/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableFundBalance").value(3500.00));
    }

    @Test
    @DisplayName("GET /api/admin/fund/reports/export returns CSV file")
    void testExportFundReport() throws Exception {
        String csvContent = "Metric,Amount (GBP)\nTotal Donations Received,5000.00\n";
        when(adminFundService.exportFundReportCsv()).thenReturn(csvContent);

        mockMvc.perform(get("/api/admin/fund/reports/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"burial_fund_financial_report.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Total Donations Received")));
    }
}
