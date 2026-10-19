package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.request.CreateDonationPaymentIntentRequest;
import com.rememberme.api.dto.response.*;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.DonationReceiptService;
import com.rememberme.api.service.DonationService;
import com.rememberme.api.service.FundLedgerService;
import com.rememberme.api.service.StripeWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DonationController.class, StripeWebhookController.class, FundController.class})
@AutoConfigureMockMvc(addFilters = false)
public class DonationControllersMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DonationService donationService;

    @MockBean
    private DonationReceiptService donationReceiptService;

    @MockBean
    private FundLedgerService fundLedgerService;

    @MockBean
    private StripeWebhookService stripeWebhookService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User donor;

    @BeforeEach
    public void setUp() {
        donor = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test Donor")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(donor));
    }

    @Test
    public void getDonationOptions_Returns200WithTiers() throws Exception {
        DonationOptionsResponse options = DonationOptionsResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .presetAmounts(List.of(
                        new PresetAmountDto(500L, "£5"),
                        new PresetAmountDto(1000L, "£10"),
                        new PresetAmountDto(5000L, "£50")
                ))
                .minCustomAmountPence(100L)
                .maxCustomAmountPence(500000L)
                .sharedFundExplanation("Every contribution directly strengthens our single Community Burial Fund.")
                .stripePublishableKey("pk_test_mock")
                .build();

        when(donationService.getDonationOptions()).thenReturn(options);

        mockMvc.perform(get("/api/v1/donations/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currency").value("GBP"))
                .andExpect(jsonPath("$.data.presetAmounts[0].amountPence").value(500))
                .andExpect(jsonPath("$.data.presetAmounts[1].amountPence").value(1000))
                .andExpect(jsonPath("$.data.minCustomAmountPence").value(100))
                .andExpect(jsonPath("$.data.maxCustomAmountPence").value(500000));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void createPaymentIntent_Success() throws Exception {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(1000L)
                .currency("GBP")
                .idempotencyKey("idem-mvc-123")
                .build();

        CreatePaymentIntentResponse response = CreatePaymentIntentResponse.builder()
                .donationId(101L)
                .clientSecret("pi_test_secret_123")
                .paymentIntentId("pi_test_123")
                .amountPence(1000L)
                .currency("GBP")
                .status("PENDING")
                .idempotencyKey("idem-mvc-123")
                .build();

        when(donationService.createPaymentIntent(eq(donor), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/donations/payment-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idem-mvc-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.donationId").value(101))
                .andExpect(jsonPath("$.data.clientSecret").value("pi_test_secret_123"))
                .andExpect(jsonPath("$.data.amountPence").value(1000))
                .andExpect(jsonPath("$.data.currency").value("GBP"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void createPaymentIntent_InvalidAmount_FailsValidation() throws Exception {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(50L) // Under minimum 100 pence (£1.00)
                .currency("GBP")
                .build();

        mockMvc.perform(post("/api/v1/donations/payment-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void getMyDonations_ReturnsPaginatedList() throws Exception {
        DonationResponse d = DonationResponse.builder()
                .id(101L)
                .amountPence(1000L)
                .amountFormatted("£10.00")
                .currency("GBP")
                .paymentStatus("SUCCEEDED")
                .settlementStatus("SETTLED")
                .hasReceipt(true)
                .receiptNumber("RCP-20261019-ABCDEF")
                .createdAt(LocalDateTime.now())
                .build();

        when(donationService.getMyDonations(eq(donor), any()))
                .thenReturn(new PageImpl<>(List.of(d)));

        mockMvc.perform(get("/api/v1/me/donations?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101))
                .andExpect(jsonPath("$.data.content[0].amountPence").value(1000))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("SUCCEEDED"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void getMyDonationById_Success() throws Exception {
        DonationResponse d = DonationResponse.builder()
                .id(101L)
                .amountPence(5000L)
                .amountFormatted("£50.00")
                .currency("GBP")
                .paymentStatus("SUCCEEDED")
                .settlementStatus("SETTLED")
                .hasReceipt(true)
                .receiptNumber("RCP-20261019-123456")
                .build();

        when(donationService.getMyDonationById(101L, 1L)).thenReturn(d);

        mockMvc.perform(get("/api/v1/me/donations/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.amountPence").value(5000));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void getMyDonationReceipt_Success() throws Exception {
        DonationReceiptResponse receipt = DonationReceiptResponse.builder()
                .receiptNumber("RCP-20261019-ABCDEF")
                .donationId(101L)
                .donorName("Test Donor")
                .donorEmail("user@test.com")
                .amountPence(1000L)
                .amountFormatted("£10.00")
                .currency("GBP")
                .paymentMethod("card")
                .fundName("Community Burial Fund")
                .issuedAt(LocalDateTime.now())
                .build();

        when(donationReceiptService.getReceiptForDonor(101L, 1L)).thenReturn(receipt);

        mockMvc.perform(get("/api/v1/me/donations/101/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNumber").value("RCP-20261019-ABCDEF"))
                .andExpect(jsonPath("$.data.amountPence").value(1000))
                .andExpect(jsonPath("$.data.fundName").value("Community Burial Fund"));
    }

    @Test
    public void handleStripeWebhook_Success() throws Exception {
        doNothing().when(stripeWebhookService).processWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=test_sig")
                        .content("{\"id\":\"evt_test_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        verify(stripeWebhookService, times(1)).processWebhook(anyString(), eq("t=123,v1=test_sig"));
    }

    @Test
    public void getFundSummary_PublicEndpointReturnsAggregates() throws Exception {
        FundSummaryResponse summary = FundSummaryResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .totalSettledDonationsCount(42L)
                .totalGrossPence(250000L)
                .totalFeesPence(7500L)
                .totalRefundsPence(0L)
                .availableBalancePence(242500L)
                .availableBalanceFormatted("£2,425.00")
                .fundPurpose("Community Burial Fund")
                .generatedAt(LocalDateTime.now())
                .build();

        when(fundLedgerService.getFundSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/fund/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currency").value("GBP"))
                .andExpect(jsonPath("$.data.totalSettledDonationsCount").value(42))
                .andExpect(jsonPath("$.data.totalGrossPence").value(250000))
                .andExpect(jsonPath("$.data.availableBalancePence").value(242500))
                .andExpect(jsonPath("$.data.availableBalanceFormatted").value("£2,425.00"));
    }
}
