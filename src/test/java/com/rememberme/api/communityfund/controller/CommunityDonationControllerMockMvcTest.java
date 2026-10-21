package com.rememberme.api.communityfund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.communityfund.dto.request.DonationCheckoutRequest;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.CommunityDonationService;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.StripeWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CommunityDonationController.class, PaymentWebhookController.class})
@AutoConfigureMockMvc(addFilters = false)
public class CommunityDonationControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommunityDonationService donationService;

    @MockBean
    private StripeWebhookService stripeWebhookService;

    @MockBean
    private CommunityFundSecurityHelper securityHelper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private User donor;

    @BeforeEach
    void setUp() {
        donor = User.builder()
                .id(1L)
                .email("donor@example.com")
                .fullName("John Smith")
                .role(User.Role.USER)
                .build();

        when(securityHelper.getCurrentAuthenticatedUser()).thenReturn(donor);
    }

    @Test
    @DisplayName("GET /api/donations/amounts returns 200 OK with presets and custom configuration")
    void testGetDonationAmounts() throws Exception {
        DonationAmountsResponse response = DonationAmountsResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .presets(List.of(
                        DonationAmountsResponse.PresetAmountOption.builder().amount(BigDecimal.valueOf(5.00)).formatted("£5.00").build(),
                        DonationAmountsResponse.PresetAmountOption.builder().amount(BigDecimal.valueOf(10.00)).formatted("£10.00").build(),
                        DonationAmountsResponse.PresetAmountOption.builder().amount(BigDecimal.valueOf(50.00)).formatted("£50.00").build()
                ))
                .customAmount(DonationAmountsResponse.CustomAmountConfig.builder()
                        .enabled(true)
                        .minAmount(BigDecimal.valueOf(1.00))
                        .maxAmount(BigDecimal.valueOf(50000.00))
                        .build())
                .explanation("Shared community burial fund")
                .build();

        when(donationService.getDonationAmounts()).thenReturn(response);

        mockMvc.perform(get("/api/donations/amounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currency").value("GBP"))
                .andExpect(jsonPath("$.data.presets").isArray())
                .andExpect(jsonPath("$.data.presets.length()").value(3));
    }

    @Test
    @DisplayName("POST /api/donations/checkout creates pending donation and returns checkout session")
    void testCreateCheckout() throws Exception {
        DonationCheckoutRequest request = DonationCheckoutRequest.builder()
                .amount(BigDecimal.valueOf(50.00))
                .currency("GBP")
                .dedication(DonationCheckoutRequest.DedicationRequest.builder()
                        .enabled(true)
                        .lovedOneNames(List.of("Uncle Arthur"))
                        .message("In peace")
                        .build())
                .build();

        DonationCheckoutResponse response = DonationCheckoutResponse.builder()
                .donationId(101L)
                .clientSecret("pi_test_secret_123")
                .stripePaymentIntentId("pi_test_123")
                .paymentProvider("STRIPE")
                .amount(BigDecimal.valueOf(50.00))
                .amountFormatted("£50.00")
                .currency("GBP")
                .paymentStatus("PENDING")
                .dedicationNames(List.of("Uncle Arthur"))
                .dedicationMessage("In peace")
                .checkoutInstructions("Pass clientSecret to Stripe")
                .build();

        when(donationService.createCheckout(eq(donor), any(DonationCheckoutRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/donations/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.donationId").value(101))
                .andExpect(jsonPath("$.data.clientSecret").value("pi_test_secret_123"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/donations/{id} returns donation details")
    void testGetDonationById() throws Exception {
        DonationDetailsResponse response = DonationDetailsResponse.builder()
                .id(101L)
                .donorName("John Smith")
                .amount(BigDecimal.valueOf(50.00))
                .amountFormatted("£50.00")
                .currency("GBP")
                .paymentStatus("PENDING")
                .clientSecret("pi_test_secret_123")
                .createdAt(LocalDateTime.now())
                .build();

        when(donationService.getDonationDetails(101L, donor)).thenReturn(response);

        mockMvc.perform(get("/api/donations/101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/donations/{id}/receipt returns confirmed receipt")
    void testGetDonationReceipt() throws Exception {
        DonationReceiptInfoResponse response = DonationReceiptInfoResponse.builder()
                .receiptNumber("RCP-20261021-X1Y2Z3")
                .donationId(101L)
                .donorName("John Smith")
                .amount(BigDecimal.valueOf(50.00))
                .currency("GBP")
                .paymentMethod("card")
                .fundName("Remember Me Community Burial Fund")
                .issuedAt(LocalDateTime.now())
                .build();

        when(donationService.getDonationReceipt(101L, donor)).thenReturn(response);

        mockMvc.perform(get("/api/donations/101/receipt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNumber").value("RCP-20261021-X1Y2Z3"))
                .andExpect(jsonPath("$.data.amount").value(50.00));
    }

    @Test
    @DisplayName("POST /api/donations/{id}/retry refreshes checkout session safely")
    void testRetryDonation() throws Exception {
        DonationRetryResponse response = DonationRetryResponse.builder()
                .donationId(101L)
                .clientSecret("pi_refreshed_secret_999")
                .stripePaymentIntentId("pi_fresh_999")
                .amount(BigDecimal.valueOf(50.00))
                .currency("GBP")
                .status("PENDING")
                .message("Session refreshed")
                .build();

        when(donationService.retryDonation(101L, donor)).thenReturn(response);

        mockMvc.perform(post("/api/donations/101/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientSecret").value("pi_refreshed_secret_999"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/payments/webhook handles webhook notification")
    void testPaymentWebhook() throws Exception {
        doNothing().when(stripeWebhookService).processWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"evt_test\"}")
                        .header("Stripe-Signature", "t=123,v1=test_sig"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        verify(stripeWebhookService, times(1)).processWebhook(anyString(), eq("t=123,v1=test_sig"));
    }

    @Test
    @DisplayName("GET /api/donations/me returns 200 OK with paginated donation history")
    void testGetMyDonations_Success() throws Exception {
        DonationDetailsResponse item1 = DonationDetailsResponse.builder()
                .id(101L)
                .donorName("John Smith")
                .amount(BigDecimal.valueOf(50.00))
                .amountFormatted("£50.00")
                .currency("GBP")
                .paymentStatus("SUCCEEDED")
                .createdAt(LocalDateTime.now())
                .build();

        Page<DonationDetailsResponse> page = new PageImpl<>(List.of(item1));
        when(donationService.getMyDonations(eq(donor), eq(0), eq(10), eq("createdAt"), eq("DESC")))
                .thenReturn(page);

        mockMvc.perform(get("/api/donations/me")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101))
                .andExpect(jsonPath("$.data.content[0].amount").value(50.00))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("SUCCEEDED"));
    }

    @Test
    @DisplayName("GET /api/donations/me with invalid pagination returns 400 Bad Request")
    void testGetMyDonations_InvalidPagination_ReturnsBadRequest() throws Exception {
        when(donationService.getMyDonations(any(), eq(-1), anyInt(), anyString(), anyString()))
                .thenThrow(new com.rememberme.api.exception.InvalidPaginationException("Page index must be non-negative and size must be between 1 and 100"));

        mockMvc.perform(get("/api/donations/me")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGINATION"));
    }
}
